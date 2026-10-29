package com.ai.exam_ai.ai;

import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiChat {

    @Autowired
    private ChatModel chatModel;

    @GetMapping("/chat/{message}")
    public String chat(@PathVariable String message) {
        return chatModel.chat(message);
    }


}
