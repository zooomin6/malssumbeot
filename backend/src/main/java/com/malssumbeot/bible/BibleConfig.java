package com.malssumbeot.bible;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BibleConfig {

    @Bean
    public BibleBookCatalog bibleBookCatalog(BibleBookRepository bookRepository) {
        return new BibleBookCatalog(bookRepository.findAll());
    }

    @Bean
    public VerseReferenceParser verseReferenceParser(BibleBookCatalog catalog) {
        return new VerseReferenceParser(catalog);
    }

    @Bean
    public VerseReferenceScanner verseReferenceScanner(VerseReferenceParser parser) {
        return new VerseReferenceScanner(parser);
    }
}
