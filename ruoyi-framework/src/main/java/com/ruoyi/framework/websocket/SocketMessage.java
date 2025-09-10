package com.ruoyi.framework.websocket;

public class SocketMessage {

    private String content;

    private int type;

    public SocketMessage(String content, int type) {
        this.content = content;
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }


    public static SocketMessage success(String content) {
        return new SocketMessage(content, 1);
    }

    public static SocketMessage error(String content) {
        return new SocketMessage(content, 2);
    }

    public static SocketMessage warn(String content) {
        return new SocketMessage(content, 3);
    }
}
