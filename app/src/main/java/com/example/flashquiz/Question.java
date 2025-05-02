package com.example.flashquiz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Question {
    private String question;
    private String correctAnswer;
    private List<String> options;

    public Question(String question, String correctAnswer, List<String> incorrectAnswers) {
        this.question = question;
        this.correctAnswer = correctAnswer;
        options = new ArrayList<>();
        options.add(correctAnswer);
        options.addAll(incorrectAnswers);
        // Mélange des options pour ne pas que la bonne réponse soit toujours au même endroit
        Collections.shuffle(options);
    }

    public String getQuestion() {
        return question;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public List<String> getOptions() {
        return options;
    }
}