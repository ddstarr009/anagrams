package anagram

import grails.rest.*
import grails.converters.*
import grails.transaction.*
import static org.springframework.http.HttpStatus.*
import static org.springframework.http.HttpMethod.*
import groovy.json.JsonSlurper
import redis.clients.jedis.exceptions.JedisConnectionException

class AnagramController {
    def anagramService // using Springs DI by convention here
	static responseFormats = ['json', 'xml']
	
    def anagramGroups() { 
        def groupsData = anagramService.fetchGroupsByMinSize(params.minSize)
        render groupsData as JSON
    }

    def mostAnagrams() { 
       def mostAnagrams = anagramService.fetchMostAnagrams() 
       render mostAnagrams as JSON
    }

    def findAnagrams() { 
		def anagrams = anagramService.findAnagramsForWord(params.word, params.limit, params.proper)
		render anagrams as JSON
	}

    def wordsStats() {
        def wordsStats = anagramService.fetchWordsStats()
        render wordsStats as JSON
    }

    def addWords() { 
		if (request.JSON) {
            def wordsToAdd = request.JSON.words
            anagramService.addToDataStore(wordsToAdd)
            render (status: 201, text: 'created')
        }
        else { // handle x-www-form-urlencoded content type
            if (request.getParameterMap()) {
                String firstKey = request.getParameterMap().keySet().iterator().next()
                def jsonSlurper = new JsonSlurper()
                def jsonObj = jsonSlurper.parseText(firstKey)
                anagramService.addToDataStore(jsonObj.words)
                render (status: 201, text: 'created')
            }
            else {
                render (status: 400, text: 'No POST data')
            }
        }
    }

    def anagramChecker() { 
        if (params.words == null) {
            render (status: 200, text: 'You did not supply any words')
        }
        else {
            def isWordsSame = anagramService.areWordsInSameFamily(params.words)
            render (status: 200, text: isWordsSame)
        }
    }

    def deleteAnagramFamily() { 
        anagramService.deleteAnagramFamily(params.word)
        render (status: 204)
    }

    def deleteAllWords() { 
        anagramService.deleteAllWords()
        render (status: 204)
    }

    def deleteWord() { 
        anagramService.deleteWord(params.word)
        render (status: 204)
    }

    def exception(MalformedURLException exception) {
        logException exception
        render (status: 400, text: exception?.message)
    }

    def jedisConnectException(JedisConnectionException exception) {
        logException exception
        render (status: 500, text: 'is Redis running on port 6379? ' + exception?.message)
    }

	/** Log exception */
    private void logException(Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
    }
}
