package com.malssumbeot.bible;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BibleVerseRepository extends JpaRepository<BibleVerse, Long> {

    List<BibleVerse> findByBookIdAndChapterAndVerseBetweenOrderByVerseAsc(
            int bookId, int chapter, int verseStart, int verseEnd);
}
