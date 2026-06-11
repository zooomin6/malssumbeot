package com.malssumbeot.bible;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "bible_book")
public class BibleBook {

    @Id
    private Integer id;

    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(name = "name_ko", nullable = false, unique = true, length = 20)
    private String nameKo;

    @Column(name = "abbr_ko", nullable = false, unique = true, length = 8)
    private String abbrKo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 2)
    private Testament testament;

    @Column(name = "chapter_count", nullable = false)
    private int chapterCount;

    protected BibleBook() {
    }

    public BibleBook(Integer id, String code, String nameKo, String abbrKo,
                     Testament testament, int chapterCount) {
        this.id = id;
        this.code = code;
        this.nameKo = nameKo;
        this.abbrKo = abbrKo;
        this.testament = testament;
        this.chapterCount = chapterCount;
    }

    public Integer getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getNameKo() {
        return nameKo;
    }

    public String getAbbrKo() {
        return abbrKo;
    }

    public Testament getTestament() {
        return testament;
    }

    public int getChapterCount() {
        return chapterCount;
    }
}
