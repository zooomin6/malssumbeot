package com.malssumbeot.bible;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "bible_verse",
        uniqueConstraints = @UniqueConstraint(name = "uq_bible_verse",
                columnNames = {"book_id", "chapter", "verse"}))
public class BibleVerse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_id", nullable = false)
    private int bookId;

    @Column(nullable = false)
    private int chapter;

    @Column(nullable = false)
    private int verse;

    @Column(nullable = false, length = 2000)
    private String text;

    protected BibleVerse() {
    }

    public BibleVerse(int bookId, int chapter, int verse, String text) {
        this.bookId = bookId;
        this.chapter = chapter;
        this.verse = verse;
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public int getBookId() {
        return bookId;
    }

    public int getChapter() {
        return chapter;
    }

    public int getVerse() {
        return verse;
    }

    public String getText() {
        return text;
    }
}
