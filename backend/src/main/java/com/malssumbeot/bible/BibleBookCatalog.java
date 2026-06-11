package com.malssumbeot.bible;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 책 이름·약어 → BibleBook 해석. 풀네임(창세기)과 표준 약어(창)를 모두 받는다.
 */
public class BibleBookCatalog {

    private final Map<String, BibleBook> byAlias;

    public BibleBookCatalog(List<BibleBook> books) {
        Map<String, BibleBook> aliases = new HashMap<>();
        for (BibleBook book : books) {
            aliases.put(normalize(book.getNameKo()), book);
            aliases.put(normalize(book.getAbbrKo()), book);
        }
        this.byAlias = Map.copyOf(aliases);
    }

    public Optional<BibleBook> resolve(String nameOrAbbr) {
        if (nameOrAbbr == null || nameOrAbbr.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byAlias.get(normalize(nameOrAbbr)));
    }

    private static String normalize(String raw) {
        return raw.replaceAll("\\s+", "");
    }
}
