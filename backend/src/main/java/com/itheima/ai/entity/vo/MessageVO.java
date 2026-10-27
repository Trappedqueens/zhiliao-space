package com.itheima.ai.entity.vo;

import lombok.Data;
import org.springframework.ai.chat.messages.Message;

@Data
public class MessageVO {
    private String role;
    private String content;

    public MessageVO(Message message){
        switch (message.getMessageType()){
            case USER:
                role = "user";
                break;
            case ASSISTANT:
                role = "assistant";
                break;
                case SYSTEM:
                role = "system";
                break;
                default:
                role = "";
                break;
        }
        this.content = message.getText();
    }
}
