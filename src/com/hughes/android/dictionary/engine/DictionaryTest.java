package com.hughes.android.dictionary.engine;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import com.hughes.android.dictionary.engine.Index.IndexEntry;
import com.hughes.android.dictionary.engine.PairEntry.Row;


public class DictionaryTest extends TestCase {

  @Override
  protected void setUp() {
    while (!TransliteratorManager.init(null)) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  public void testEnItWiktionary() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("dictOutputs/EN-IT_enwiktionary.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index enIndex = dict.indices.get(0);
    
    final PairEntry.Row row = (Row) enIndex.rows.get(2);
    assertEquals("z", row.getRawText(false));

    raf.close();
  }

  public void testGermanMetadata() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    assertEquals("de", deIndex.shortName);
    assertEquals("de->en", deIndex.longName);
    
    assertEquals(2, dict.sources.size());
    assertEquals("chemnitz", dict.sources.get(0).name);
    assertEquals(0, dict.sources.get(0).pairEntryStart);
    assertEquals("dictcc", dict.sources.get(1).name);
    assertEquals(113, dict.sources.get(1).pairEntryStart);
    
    raf.close();
  }
  
  public void testGermanIndex() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    for (final Index.IndexEntry indexEntry : deIndex.sortedIndexEntries) {
      System.out.println("testing: " + indexEntry.token);
      final IndexEntry searchResult = deIndex.findInsertionPoint(indexEntry.token, new AtomicBoolean(
          false));
      assertEquals("Looked up: " + indexEntry.token, indexEntry.token.toLowerCase(), searchResult.token.toLowerCase());
    }

    // TODO: maybe if user types capitalization, use it.
    assertSearchResult("aaac", "aaac", deIndex.findInsertionPoint("aaac", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findInsertionPoint("AAAC", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findInsertionPoint("AAAc", new AtomicBoolean(false)));
    assertSearchResult("aaac", "aaac", deIndex.findInsertionPoint("aAac", new AtomicBoolean(false)));

    // Before the beginning.
    assertSearchResult("40", "40" /* special case */, deIndex.findInsertionPoint("", new AtomicBoolean(false)));
    assertSearchResult("40", "40" /* special case */, deIndex.findInsertionPoint("__", new AtomicBoolean(false)));
    
    // After the end.
    assertSearchResult("Zweckorientiertheit", "zählen", deIndex.findInsertionPoint("ZZZZZ", new AtomicBoolean(false)));

    assertSearchResult("ab", "aaac", deIndex.findInsertionPoint("aaaca", new AtomicBoolean(false)));
    assertSearchResult("machen", "machen", deIndex.findInsertionPoint("m", new AtomicBoolean(false)));
    assertSearchResult("machen", "machen", deIndex.findInsertionPoint("macdddd", new AtomicBoolean(false)));


    assertSearchResult("überprüfe", "überprüfe", deIndex.findInsertionPoint("ueberprüfe", new AtomicBoolean(false)));
    assertSearchResult("überprüfe", "überprüfe", deIndex.findInsertionPoint("ueberpruefe", new AtomicBoolean(false)));

    assertSearchResult("überprüfe", "überprüfe", deIndex.findInsertionPoint("ueberpBLEH", new AtomicBoolean(false)));
    assertSearchResult("überprüfe", "überprüfe", deIndex.findInsertionPoint("überprBLEH", new AtomicBoolean(false)));

    assertSearchResult("überprüfen", "überprüfe", deIndex.findInsertionPoint("überprüfeBLEH", new AtomicBoolean(false)));

    // Check that search in lowercase works.
    assertSearchResult("Alibi", "Alibi", deIndex.findInsertionPoint("alib", new AtomicBoolean(false)));
    System.out.println(deIndex.findInsertionPoint("alib", new AtomicBoolean(false)).toString());
    
    raf.close();
  }
  
  private void assertSearchResult(final String insertionPoint, final String longestPrefix,
      final IndexEntry actual) {
    assertEquals(insertionPoint, actual.token);
  }

  public void testGermanTokenRows() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("testdata/de-en.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    // Pre-cache a few of these, just to make sure that's working.
    for (int i = 0; i < deIndex.rows.size(); i += 7) {
      deIndex.rows.get(i).getTokenRow(true);
    }
    
    // Do the exhaustive searching.
    TokenRow lastTokenRow = null;
    for (final RowBase row : deIndex.rows) {
      if (row instanceof TokenRow) {
        lastTokenRow = (TokenRow) row;
      }
      assertEquals(lastTokenRow, row.getTokenRow(true));
    }

    // Now they're all cached, we shouldn't have to search.
    for (final RowBase row : deIndex.rows) {
      if (row instanceof TokenRow) {
        lastTokenRow = (TokenRow) row;
      }
      // This will break if the Row cache isn't big enough.
      assertEquals(lastTokenRow, row.getTokenRow(false));
    }
    
    raf.close();
  }
  
  public void testChemnitz() throws IOException {
    final RandomAccessFile raf = new RandomAccessFile("dictOutputs/de-en_chemnitz.quickdic", "r");
    final Dictionary dict = new Dictionary(raf);
    final Index deIndex = dict.indices.get(0);
    
    assertSearchResult("Höschen", "Hos", deIndex.findInsertionPoint("Hos", new AtomicBoolean(false)));
    assertSearchResult("Höschen", "hos", deIndex.findInsertionPoint("hos", new AtomicBoolean(false)));

    raf.close();
  }


}
