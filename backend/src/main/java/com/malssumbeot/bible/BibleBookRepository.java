package com.malssumbeot.bible;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BibleBookRepository extends JpaRepository<BibleBook, Integer> {

    java.util.Optional<BibleBook> findByAbbrKo(String abbrKo);
}
