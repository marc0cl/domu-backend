package com.domu.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private Long roomId;
    private String content;
    private String type; // TEXT, IMAGE, AUDIO
    private String boxFileId;
}
