package anagram


import grails.rest.*
import grails.converters.*
import grails.transaction.*
import static org.springframework.http.HttpStatus.*
import static org.springframework.http.HttpMethod.*
import groovy.json.JsonSlurper

class AnagramController {
    def anagramService // using Springs DI by convention here
	static responseFormats = ['json', 'xml']
	
    //def index() { }

    def show() { 
		def anagrams = anagramService.findAnagramsForWord(params.word, params.limit, params.proper)
		render anagrams as JSON
	}

    def save() { 
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

    def delete() { 
        anagramService.deleteWord(params.word)
        render (status: 204)
    }

    def exception(Exception exception) {
        logException exception
        render (status: 400, text: exception?.message)
    }

	/** Log exception */
    private void logException(Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
    }
}
