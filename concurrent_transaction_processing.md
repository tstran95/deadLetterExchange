# Xử Lý Giao Dịch Đồng Thời và Quản Lý Số Dư Ngân Hàng

## 1. Vấn Đề Concurrency trong Banking

### 1.1 Race Condition Ví Dụ
```
Thời điểm T1: Số dư tài khoản A = 1000
- Giao dịch 1: Rút 300 (đang xử lý)
- Giao dịch 2: Rút 500 (đang xử lý)

Kết quả không mong muốn:
- Cả 2 giao dịch đều đọc số dư = 1000
- Giao dịch 1: 1000 - 300 = 700
- Giao dịch 2: 1000 - 500 = 500
- Số dư cuối: 500 (sai, phải là 200)
```

## 2. Các Kỹ Thuật Xử Lý Concurrency

### 2.1 Database Locking Mechanisms

#### 2.1.1 Row-Level Locking
```sql
-- Pessimistic Locking
BEGIN TRANSACTION;
SELECT balance FROM accounts 
WHERE account_id = 'A001' 
FOR UPDATE;  -- Khóa hàng này

UPDATE accounts 
SET balance = balance - 300 
WHERE account_id = 'A001';
COMMIT;
```

#### 2.1.2 Optimistic Locking
```sql
-- Sử dụng version number
CREATE TABLE accounts (
    account_id VARCHAR(20) PRIMARY KEY,
    balance DECIMAL(15,2),
    version INTEGER DEFAULT 1
);

-- Kiểm tra version trước khi update
UPDATE accounts 
SET balance = 700, version = version + 1
WHERE account_id = 'A001' 
AND version = 1;  -- Chỉ update nếu version chưa thay đổi
```

### 2.2 Distributed Locking

#### 2.2.1 Redis Distributed Lock
```java
// Sử dụng Redis để distributed locking
public class DistributedLockManager {
    private RedisTemplate<String, String> redisTemplate;
    
    public boolean acquireLock(String accountId, String lockId, int timeoutSeconds) {
        String lockKey = "lock:account:" + accountId;
        String result = redisTemplate.opsForValue()
            .setIfAbsent(lockKey, lockId, Duration.ofSeconds(timeoutSeconds));
        return "OK".equals(result);
    }
    
    public void releaseLock(String accountId, String lockId) {
        String lockKey = "lock:account:" + accountId;
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                       "return redis.call('del', KEYS[1]) else return 0 end";
        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), 
                            Arrays.asList(lockKey), lockId);
    }
}
```

#### 2.2.2 ZooKeeper Distributed Lock
```java
public class ZooKeeperLock {
    private ZooKeeper zooKeeper;
    private String lockPath;
    
    public boolean acquireLock(String accountId) {
        try {
            lockPath = "/locks/account/" + accountId;
            zooKeeper.create(lockPath, new byte[0], 
                           ZooDefs.Ids.OPEN_ACL_UNSAFE, 
                           CreateMode.EPHEMERAL);
            return true;
        } catch (KeeperException.NodeExistsException e) {
            return false; // Lock đã được giữ bởi process khác
        }
    }
}
```

## 3. Kiến Trúc Xử Lý Giao Dịch

### 3.1 Event Sourcing Pattern
```java
// Event Store
public class TransactionEvent {
    private String eventId;
    private String accountId;
    private BigDecimal amount;
    private String transactionType;
    private LocalDateTime timestamp;
    private long version;
}

// Event Store Implementation
public class EventStore {
    public void appendEvent(String accountId, TransactionEvent event) {
        // Lưu event vào database
        String sql = "INSERT INTO transaction_events " +
                    "(account_id, event_id, amount, type, timestamp, version) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
        // Execute với optimistic locking
    }
    
    public List<TransactionEvent> getEvents(String accountId) {
        // Lấy tất cả events của account để tính balance
        String sql = "SELECT * FROM transaction_events " +
                    "WHERE account_id = ? ORDER BY version";
        // Return events
    }
}
```

### 3.2 CQRS (Command Query Responsibility Segregation)
```java
// Command Side - Xử lý giao dịch
@Service
public class TransactionCommandService {
    
    @Transactional
    public void processTransaction(TransactionCommand command) {
        // 1. Validate transaction
        validateTransaction(command);
        
        // 2. Acquire lock
        String lockId = UUID.randomUUID().toString();
        if (!lockManager.acquireLock(command.getAccountId(), lockId, 30)) {
            throw new ConcurrentTransactionException();
        }
        
        try {
            // 3. Create event
            TransactionEvent event = new TransactionEvent(
                command.getAccountId(),
                command.getAmount(),
                command.getType()
            );
            
            // 4. Store event
            eventStore.appendEvent(command.getAccountId(), event);
            
            // 5. Publish event
            eventPublisher.publish(event);
            
        } finally {
            // 6. Release lock
            lockManager.releaseLock(command.getAccountId(), lockId);
        }
    }
}

// Query Side - Đọc số dư
@Service
public class BalanceQueryService {
    
    @Cacheable("account-balances")
    public BigDecimal getBalance(String accountId) {
        List<TransactionEvent> events = eventStore.getEvents(accountId);
        return events.stream()
            .map(event -> calculateBalanceImpact(event))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

## 4. Message Queue và Event Processing

### 4.1 Apache Kafka Implementation
```java
// Transaction Producer
@Component
public class TransactionProducer {
    
    @Autowired
    private KafkaTemplate<String, TransactionEvent> kafkaTemplate;
    
    public void publishTransaction(TransactionEvent event) {
        kafkaTemplate.send("transactions", event.getAccountId(), event);
    }
}

// Transaction Consumer
@Component
public class TransactionConsumer {
    
    @KafkaListener(topics = "transactions", groupId = "balance-processor")
    public void processTransaction(TransactionEvent event) {
        // Xử lý giao dịch theo thứ tự
        updateBalance(event);
        sendNotification(event);
        updateAnalytics(event);
    }
    
    private void updateBalance(TransactionEvent event) {
        // Update balance với proper locking
        String lockKey = "balance:" + event.getAccountId();
        redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", 30);
        
        try {
            // Update balance
            BigDecimal newBalance = calculateNewBalance(event);
            balanceRepository.updateBalance(event.getAccountId(), newBalance);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }
}
```

### 4.2 Redis Streams cho Real-time Processing
```java
// Producer
public class TransactionStreamProducer {
    
    public void addTransaction(TransactionEvent event) {
        Map<String, String> fields = new HashMap<>();
        fields.put("accountId", event.getAccountId());
        fields.put("amount", event.getAmount().toString());
        fields.put("type", event.getType());
        
        redisTemplate.opsForStream().add("transactions", fields);
    }
}

// Consumer Group
@Component
public class TransactionStreamConsumer {
    
    @PostConstruct
    public void startConsuming() {
        while (true) {
            List<MapRecord<String, Object, Object>> records = 
                redisTemplate.opsForStream().read(
                    Consumer.from("balance-processor", "consumer-1"),
                    StreamReadOptions.empty().count(100),
                    StreamOffset.create("transactions", ReadOffset.lastConsumed())
                );
            
            for (MapRecord<String, Object, Object> record : records) {
                processTransaction(record);
                // Acknowledge message
                redisTemplate.opsForStream().acknowledge("transactions", 
                    "balance-processor", record.getId());
            }
        }
    }
}
```

## 5. Database Sharding và Partitioning

### 5.1 Account-based Sharding
```java
public class ShardingStrategy {
    
    public String getShardKey(String accountId) {
        // Hash accountId để phân tán đều
        int hash = accountId.hashCode();
        return "shard_" + (Math.abs(hash) % 10);
    }
    
    public DataSource getDataSource(String accountId) {
        String shardKey = getShardKey(accountId);
        return dataSourceMap.get(shardKey);
    }
}

// Transaction Service với Sharding
@Service
public class ShardedTransactionService {
    
    public void processTransaction(TransactionCommand command) {
        String shardKey = shardingStrategy.getShardKey(command.getAccountId());
        
        // Sử dụng connection pool cho shard cụ thể
        try (Connection conn = shardingStrategy.getDataSource(command.getAccountId()).getConnection()) {
            conn.setAutoCommit(false);
            
            // Acquire lock trên shard cụ thể
            acquireShardLock(conn, command.getAccountId());
            
            // Process transaction
            processTransactionOnShard(conn, command);
            
            conn.commit();
        } catch (Exception e) {
            // Rollback và retry logic
        }
    }
}
```

## 6. Circuit Breaker và Retry Patterns

### 6.1 Circuit Breaker Implementation
```java
@Component
public class TransactionCircuitBreaker {
    
    private CircuitBreaker circuitBreaker;
    
    public TransactionCircuitBreaker() {
        this.circuitBreaker = CircuitBreaker.builder()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .ringBufferSizeInHalfOpenState(2)
            .ringBufferSizeInClosedState(10)
            .build();
    }
    
    public CompletableFuture<TransactionResult> processTransaction(TransactionCommand command) {
        return circuitBreaker.executeAsync(() -> {
            return CompletableFuture.supplyAsync(() -> {
                return transactionService.processTransaction(command);
            });
        });
    }
}
```

### 6.2 Retry Pattern
```java
@Service
public class RetryableTransactionService {
    
    @Retryable(
        value = {ConcurrentTransactionException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public TransactionResult processTransactionWithRetry(TransactionCommand command) {
        return transactionService.processTransaction(command);
    }
    
    @Recover
    public TransactionResult recover(ConcurrentTransactionException e, TransactionCommand command) {
        // Fallback logic - có thể queue transaction để xử lý sau
        return queueTransactionForLaterProcessing(command);
    }
}
```

## 7. Monitoring và Observability

### 7.1 Metrics Collection
```java
@Component
public class TransactionMetrics {
    
    private final MeterRegistry meterRegistry;
    private final Counter transactionCounter;
    private final Timer transactionTimer;
    private final Gauge activeTransactionsGauge;
    
    public TransactionMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.transactionCounter = Counter.builder("transactions.total")
            .description("Total number of transactions")
            .register(meterRegistry);
        this.transactionTimer = Timer.builder("transactions.duration")
            .description("Transaction processing time")
            .register(meterRegistry);
        this.activeTransactionsGauge = Gauge.builder("transactions.active")
            .description("Active transactions")
            .register(meterRegistry, this, TransactionMetrics::getActiveTransactions);
    }
    
    public void recordTransaction(String type, long duration) {
        transactionCounter.increment();
        transactionTimer.record(duration, TimeUnit.MILLISECONDS);
    }
}
```

### 7.2 Distributed Tracing
```java
@Service
public class TracedTransactionService {
    
    @Autowired
    private Tracer tracer;
    
    public TransactionResult processTransaction(TransactionCommand command) {
        Span span = tracer.buildSpan("process-transaction")
            .withTag("account.id", command.getAccountId())
            .withTag("transaction.type", command.getType())
            .start();
        
        try (Scope scope = tracer.activateSpan(span)) {
            // Process transaction
            TransactionResult result = doProcessTransaction(command);
            
            span.setTag("transaction.success", true);
            return result;
        } catch (Exception e) {
            span.setTag("transaction.success", false);
            span.setTag("error.message", e.getMessage());
            throw e;
        } finally {
            span.finish();
        }
    }
}
```

## 8. Best Practices

### 8.1 Idempotency
```java
public class IdempotentTransactionService {
    
    public TransactionResult processTransaction(TransactionCommand command) {
        String idempotencyKey = command.getIdempotencyKey();
        
        // Kiểm tra xem transaction đã được xử lý chưa
        TransactionResult existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return existing;
        }
        
        // Process transaction
        TransactionResult result = doProcessTransaction(command);
        
        // Lưu idempotency key
        transactionRepository.saveIdempotencyKey(idempotencyKey, result);
        
        return result;
    }
}
```

### 8.2 Saga Pattern cho Complex Transactions
```java
public class TransferSaga {
    
    public void executeTransfer(TransferCommand command) {
        Saga saga = new Saga("transfer-" + command.getTransferId());
        
        try {
            // Step 1: Debit from source account
            saga.addStep("debit-source", () -> debitAccount(command.getSourceAccount(), command.getAmount()));
            
            // Step 2: Credit to destination account
            saga.addStep("credit-destination", () -> creditAccount(command.getDestinationAccount(), command.getAmount()));
            
            // Step 3: Update transfer status
            saga.addStep("update-status", () -> updateTransferStatus(command.getTransferId(), "COMPLETED"));
            
            saga.execute();
        } catch (Exception e) {
            saga.compensate();
            throw e;
        }
    }
}
```

## 9. Kết Luận

Để xử lý nhiều giao dịch đồng thời, ngân hàng sử dụng:

1. **Locking Mechanisms**: Row-level locks, optimistic locking
2. **Distributed Systems**: Redis locks, ZooKeeper coordination
3. **Event Sourcing**: Immutable event log
4. **Message Queues**: Kafka, Redis Streams
5. **Sharding**: Phân tán load theo account
6. **Circuit Breakers**: Xử lý lỗi gracefully
7. **Monitoring**: Metrics, tracing, observability
8. **Idempotency**: Tránh duplicate processing
9. **Saga Pattern**: Xử lý complex transactions

Các kỹ thuật này đảm bảo:
- **Consistency**: Số dư luôn chính xác
- **Availability**: Hệ thống luôn sẵn sàng
- **Performance**: Xử lý được volume lớn
- **Reliability**: Không mất giao dịch