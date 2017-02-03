package anagram

class UrlMappings {

    static mappings = {
        "/"(controller: 'application', action:'index')
        "500"(view: '/error')
        "404"(view: '/notFound')

        // anagram mappings

        // GET anagrams for a word
        "/api/v1/anagrams/$word"(controller: 'anagram', action:'show', method: 'GET')

        // GET that takes a set of words and returns whether or not they are all anagrams of each other
        "/api/v1/anagrams/checker"(controller: 'anagram', action:'anagramChecker', method: 'GET')

        // GET that returns a count of words in the corpus and min/max/median/average word length
        "/api/v1/words/stats"(controller: 'anagram', action:'wordsStats', method: 'GET')

		// POST for adding words to dictionary
        "/api/v1/words"(controller: 'anagram', action:'save', method:'POST')

        // DELETE a single word from data store
        "/api/v1/words/$word"(controller: 'anagram', action:'delete', method:'DELETE')
        
        // DELETE all contents of data store
        "/api/v1/words"(controller: 'anagram', action:'deleteAllWords', method:'DELETE')

        // DELETE a word and all of its anagrams
        "/api/v1/anagrams/$word"(controller: 'anagram', action:'deleteAnagramFamily', method:'DELETE')

    }
}
