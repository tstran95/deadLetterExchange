package com.vn.deadletter.model;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Product {
    private String barCode;
    private String color;
    private Integer size;
    private String urlImage;
    private String sku;

    public Product() {
    }

    public Product(String barCode, String color, Integer size, String urlImage, String sku) {
        this.barCode = barCode;
        this.color = color;
        this.size = size;
        this.urlImage = urlImage;
        this.sku = sku;
    }

    public String getBarCode() {
        return barCode;
    }

    public void setBarCode(String barCode) {
        this.barCode = barCode;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getUrlImage() {
        return urlImage;
    }

    public void setUrlImage(String urlImage) {
        this.urlImage = urlImage;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    @Override
    public String toString() {
        return "Product{" +
                "barCode='" + barCode + '\'' +
                ", color='" + color + '\'' +
                ", size=" + size +
                ", urlImage='" + urlImage + '\'' +
                ", sku='" + sku + '\'' +
                '}';
    }
}
