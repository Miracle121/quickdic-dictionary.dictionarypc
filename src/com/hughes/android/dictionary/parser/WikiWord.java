package com.hughes.android.dictionary.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.hughes.android.dictionary.engine.DictionaryBuilder;
import com.hughes.android.dictionary.engine.EntryData;
import com.hughes.android.dictionary.engine.EntryTypeName;
import com.hughes.android.dictionary.engine.IndexBuilder;
import com.hughes.android.dictionary.engine.PairEntry;
import com.hughes.android.dictionary.engine.PairEntry.Pair;
import com.hughes.util.ListUtil;

public class WikiWord {
  final int depth;
  
  final String title;
  String language;

  int index;
  
  final Map<String, StringBuilder> accentToPronunciation = new LinkedHashMap<String, StringBuilder>();
  StringBuilder currentPronunciation = null;

  final List<PartOfSpeech> partsOfSpeech = new ArrayList<WikiWord.PartOfSpeech>();
  
  public WikiWord(final String title, int depth) {
    this.title = title.intern();
    this.depth = depth;
  }

  static class PartOfSpeech {
    final int depth;
    final String name;

    final List<Meaning> meanings = new ArrayList<WikiWord.Meaning>();
    
    final List<TranslationSense> translationSenses = new ArrayList<WikiWord.TranslationSense>();
    
    final List<FormOf> formOfs = new ArrayList<WikiWord.FormOf>();
    
    public PartOfSpeech(final int depth, String name) {
      this.depth = depth;
      this.name = name.intern();
    }

    public Meaning newMeaning() {
      final Meaning meaning = new Meaning();
      meanings.add(meaning);
      return meaning;
    }

    public Meaning lastMeaning() {
      return meanings.isEmpty() ? newMeaning() : ListUtil.getLast(meanings);
    }
  }
  
  static class TranslationSense {
    String sense;
    List<List<Translation>> translations = new ArrayList<List<Translation>>();
    {
      translations.add(new ArrayList<Translation>());
      translations.add(new ArrayList<Translation>());
    }
  }
  
  static class Translation {
    String language;
    String text;
    
    public Translation(final String language, final String text) {
      this.language = language;
      this.text = text;
    }

    @Override
    public String toString() {
      return language + ": " + text;
    }
  }
  
  static class FormOf {
    final String grammarForm;
    final String target;
    
    public FormOf(final String grammarForm, final String token) {
      this.grammarForm = grammarForm;
      this.target = token;
    }
  }
  
  static class Meaning {
    String meaning;
    final List<Example> examples = new ArrayList<WikiWord.Example>();
    
    public Example newExample() {
      final Example example = new Example();
      this.examples.add(example);
      return example;
    }

    public Example lastExample() {
      return examples.isEmpty() ? newExample() : ListUtil.getLast(examples);
    }
  }
  
  static class Example {
    String source;
    final StringBuilder example = new StringBuilder();
    final StringBuilder exampleInEnglish = new StringBuilder();
  }
  
  // -------------------------------------------------------------------------
  
  void wikiWordToQuickDic(final DictionaryBuilder dictBuilder, final int enIndexBuilder) {
    //System.out.println("\n" + title + ", " + language + ", pron=" + accentToPronunciation);
     if (partsOfSpeech.isEmpty() && title.indexOf(":") == -1 && !language.equals("Translingual")) {
       System.err.println("Word with no POS: " + title);
     }
     for (final WikiWord.PartOfSpeech partOfSpeech : partsOfSpeech) {
       partOfSpeechToQuickDic(dictBuilder, enIndexBuilder, partOfSpeech);
     }  // PartOfSpeech

     // Pronunciation.
     if (index != -1) {
       final PairEntry pronEntry = new PairEntry();
       for (final Map.Entry<String, StringBuilder> accentToPron : accentToPronunciation.entrySet()) {
         String accent = accentToPron.getKey();
         if (accent.length() > 0) {
           accent = accent + ": ";
         }         
         pronEntry.pairs.add(new Pair(accent + accentToPron.getValue(), "", index != 0));
       }
       if (pronEntry.pairs.size() > 0) {
         final EntryData entryData = new EntryData(dictBuilder.dictionary.pairEntries.size(), pronEntry);
         dictBuilder.dictionary.pairEntries.add(pronEntry);
         final Set<String> tokens = DictFileParser.tokenize(title, DictFileParser.NON_CHAR);
         dictBuilder.indexBuilders.get(index).addEntryWithTokens(entryData, tokens, EntryTypeName.WIKTIONARY_PRONUNCIATION);
       }
     }
  }


  static final Pattern templateName = Pattern.compile("\\{[^,]*,");
  private void partOfSpeechToQuickDic(final DictionaryBuilder dictBuilder,
      final int enIndexBuilder, final WikiWord.PartOfSpeech partOfSpeech) {
    //System.out.println("  pos: " + partOfSpeech.name);
         
     for (final WikiWord.Meaning meaning : partOfSpeech.meanings) {
       //System.out.println("    meaning: " + meaning.meaning);
       for (final WikiWord.Example example : meaning.examples) {
         if (example.example.length() > 0) {
           //System.out.println("      example: " + example.example);
         }
         if (example.exampleInEnglish.length() > 0) {
           //System.out.println("      exampleInEnglish: " + example.exampleInEnglish);
         }
       }
     }
     
     if (index != -1) {
       final boolean formOfSwap = index != 0;
       for (final FormOf formOf : partOfSpeech.formOfs) {
         final Pair pair = new Pair(title + ": " + formOf.grammarForm + ": " + formOf.target, "", formOfSwap);
         final PairEntry pairEntry = new PairEntry();
         pairEntry.pairs.add(pair);
         final EntryData entryData = new EntryData(dictBuilder.dictionary.pairEntries.size(), pairEntry);
         dictBuilder.dictionary.pairEntries.add(pairEntry);
  
         // File under title token.
         final Set<String> tokens = DictFileParser.tokenize(formOf.target, DictFileParser.NON_CHAR);
         dictBuilder.indexBuilders.get(index).addEntryWithTokens(entryData, tokens, EntryTypeName.WIKTIONARY_FORM_OF);
       }
     }

     
     if (enIndexBuilder != -1 && index != -1 && enIndexBuilder != index) {
       final String entryBase = title + " (" + partOfSpeech.name.toLowerCase() + ")";
       final boolean swap = enIndexBuilder == 1;
     
       // Meanings.
       for (final Meaning meaning : partOfSpeech.meanings) {
         final PairEntry pairEntry = new PairEntry();
         final List<Pair> pairs = pairEntry.pairs;

         final List<Set<String>> exampleTokens = new ArrayList<Set<String>>();
         exampleTokens.add(new LinkedHashSet<String>());
         exampleTokens.add(new LinkedHashSet<String>());
         
         if (meaning.meaning != null && meaning.meaning.length() > 0) {
           final Pair meaningPair = new Pair(meaning.meaning, entryBase, swap);
           pairs.add(meaningPair);
         } else {
           System.err.println("Empty meaning: " + title + ", " + language + ", " + partOfSpeech.name);
         }
           
         // Examples
         for (final Example example : meaning.examples) {
           final int dashIndex = example.example.indexOf("—");
           if (example.exampleInEnglish.length() == 0 && dashIndex != -1) {
             System.out.println("Splitting example: title=" + title + ", "+ example.example);
             example.exampleInEnglish.append(example.example.substring(dashIndex + 1).trim());
             example.example.delete(dashIndex, example.example.length());
           }
           
           if (example.example.length() > 0 && example.exampleInEnglish.length() > 0) {
             final Pair pair = new Pair(example.exampleInEnglish.toString(), example.example.toString(), swap);
             pairs.add(pair);
             
             for (int i = 0; i < 2; ++i) {
               exampleTokens.get(i).addAll(DictFileParser.tokenize(pair.get(i), DictFileParser.NON_CHAR));
             }
           }
         }

         // Create EntryData with the PairEntry.
         final EntryData entryData = new EntryData(dictBuilder.dictionary.pairEntries.size(), pairEntry);
         dictBuilder.dictionary.pairEntries.add(pairEntry);

         // File under title token.
         final Set<String> titleTokens = DictFileParser.tokenize(title, DictFileParser.NON_CHAR);
         dictBuilder.indexBuilders.get(index).addEntryWithTokens(entryData, titleTokens, titleTokens.size() == 1 ? EntryTypeName.WIKTIONARY_TITLE_ONE_WORD : EntryTypeName.WIKTIONARY_TITLE_MULTI_WORD);
       
         // File under the meaning tokens (English):
         if (meaning.meaning != null) {
           // If the meaning contains any templates, strip out the template name
           // so we don't index it.
           final String meaningToIndex = templateName.matcher(meaning.meaning).replaceAll("");
           final Set<String> meaningTokens = DictFileParser.tokenize(meaningToIndex, DictFileParser.NON_CHAR);
           dictBuilder.indexBuilders.get(enIndexBuilder).addEntryWithTokens(entryData, meaningTokens, meaningTokens.size() == 1 ? EntryTypeName.WIKTIONARY_MEANING_ONE_WORD : EntryTypeName.WIKTIONARY_MEANING_MULTI_WORD);
         }
         
         // File under other tokens that we saw.
         for (int i = 0; i < 2; ++i) {
           dictBuilder.indexBuilders.get(i).addEntryWithTokens(entryData, exampleTokens.get(i), EntryTypeName.WIKTIONARY_EXAMPLE_OTHER_WORDS);
         }         
       
         
       }  // Meanings.
       
     }
     
     translationSensesToQuickDic(dictBuilder, enIndexBuilder, partOfSpeech);
  }


  private void translationSensesToQuickDic(final DictionaryBuilder dictBuilder,
      final int enIndexBuilder, final WikiWord.PartOfSpeech partOfSpeech) {
    if (!partOfSpeech.translationSenses.isEmpty()) {
       if (!language.equals("English")) {
         System.err.println("Translation sections not in English.");
       }
       
       final String englishBase = title + " (" + partOfSpeech.name.toLowerCase() + "%s)";
       
       for (final TranslationSense translationSense : partOfSpeech.translationSenses) {
         //System.out.println("    sense: " + translationSense.sense);
         if (translationSense.sense == null) {
           //System.err.println("    null sense: " + title);
         }
         String englishSense = String.format(englishBase, translationSense.sense != null ? (": " + translationSense.sense) : "");
         
         final StringBuilder[] sideBuilders = new StringBuilder[2];
         final List<Map<EntryTypeName, List<String>>> sideTokens = new ArrayList<Map<EntryTypeName,List<String>>>();
         for (int i = 0; i < 2; ++i) {
           sideBuilders[i] = new StringBuilder();
           sideTokens.add(new LinkedHashMap<EntryTypeName, List<String>>());
         }
         
         if (enIndexBuilder != -1) {
           sideBuilders[enIndexBuilder].append(englishSense);
           addTokens(title, sideTokens.get(enIndexBuilder), EntryTypeName.WIKTIONARY_TITLE_ONE_WORD);
         }
         
         // Get the entries from the translation section.
         for (int i = 0; i < 2; ++i) {
           //System.out.println("      lang: " + i);
           for (final Translation translation : translationSense.translations.get(i)) {
             //System.out.println("        translation: " + translation);
             sideBuilders[i].append(sideBuilders[i].length() > 0 ? "\n" : "");
             if (translationSense.translations.get(i).size() > 1) {
               sideBuilders[i].append(translation.language).append(": ");
             }
             sideBuilders[i].append(translation.text);
             
             // TODO: Don't index {m}, {f}
             // TODO: Don't even show: (1), (1-2), etc.
             addTokens(translation.text, sideTokens.get(i), EntryTypeName.WIKTIONARY_TRANSLATION_ONE_WORD);
           }
         }

         // Construct the Translations-based QuickDic entry for this TranslationSense.
         if (sideBuilders[0].length() > 0 && sideBuilders[1].length() > 0) {
           final Pair pair = new Pair(sideBuilders[0].toString(), sideBuilders[1].toString());
           final PairEntry pairEntry = new PairEntry();
           pairEntry.pairs.add(pair);
           final EntryData entryData = new EntryData(dictBuilder.dictionary.pairEntries.size(), pairEntry);
           dictBuilder.dictionary.pairEntries.add(pairEntry);
           
           // Add the EntryData to the indices under the correct tokens.
           for (int i = 0; i < 2; ++i) {
             final IndexBuilder indexBuilder = dictBuilder.indexBuilders.get(i);
             for (final Map.Entry<EntryTypeName, List<String>> entry : sideTokens.get(i).entrySet()) {
               for (final String token : entry.getValue()) {
                 final List<EntryData> entries = indexBuilder.getOrCreateEntries(token, entry.getKey());
                 entries.add(entryData);
               }
             }

           }             
           
         }
       }  // Senses
     }  // Translations
  }

  
  static void addTokens(final String text, final Map<EntryTypeName, List<String>> map,
      EntryTypeName entryTypeName) {
    final Set<String> tokens = DictFileParser.tokenize(text, DictFileParser.NON_CHAR);
    if (tokens.size() > 1 && entryTypeName == EntryTypeName.WIKTIONARY_TITLE_ONE_WORD) {
      entryTypeName = EntryTypeName.WIKTIONARY_TITLE_MULTI_WORD;
    }
    List<String> tokenList = map.get(entryTypeName);
    if (tokenList == null) {
      tokenList = new ArrayList<String>();
      map.put(entryTypeName, tokenList);
    }
    tokenList.addAll(tokens);
  }



}