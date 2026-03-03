package com.domu.dto;

public class ChatMessageRequest {
    private Long roomId;
    private String content;
    private String type; // TEXT, IMAGE, AUDIO
    private String boxFileId;

    public ChatMessageRequest() {}

    public ChatMessageRequest(Long roomId, String content, String type, String boxFileId) {
        this.roomId = roomId;
        this.content = content;
        this.type = type;
        this.boxFileId = boxFileId;
    }

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long roomId) { this.roomId = roomId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getBoxFileId() { return boxFileId; }
    public void setBoxFileId(String boxFileId) { this.boxFileId = boxFileId; }
}
