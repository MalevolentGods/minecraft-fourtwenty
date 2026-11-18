package com.malevolentgods.fourtwenty.domain.handbook;

/** A run of text with one style: bold, italic, a palette colour, a link to an entry, or hidden behind a terpene discovery. */
public record Span(String text, boolean bold, boolean italic, String colour, String link, String spoiler) {}
