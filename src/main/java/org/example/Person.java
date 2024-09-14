package org.example;

public class Person {
    private String address;
    private Country country;
    private String wishes;
    private long chatId;
    private String username; // Имя пользователя
    private boolean canSendAnywhere;

    public Person(long chatId) {
        this.chatId = chatId;
        this.canSendAnywhere = false;
    }
    public boolean canSendAnywhere() {
        return canSendAnywhere;
    }

    public void setCanSendAnywhere(boolean canSendAnywhere) {
        this.canSendAnywhere = canSendAnywhere;
    }

    public long getChatId() {
        return chatId;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public String getWishes() {
        return wishes;
    }

    public void setWishes(String wishes) {
        this.wishes = wishes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
