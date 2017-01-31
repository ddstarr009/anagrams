package anagram


import grails.rest.*
import grails.converters.*
import grails.transaction.*
import static org.springframework.http.HttpStatus.*
import static org.springframework.http.HttpMethod.*

class AnagramController {
    def anagramService // using Springs DI by convention here
	static responseFormats = ['json', 'xml']
	
    //def index() { }

    // TODO, unit test
    def show() { 
		def anagrams = anagramService.findAnagramsForWord(params.word, params.limit)
		render anagrams as JSON
	}

    def save() { 
        // TODO, deal with content type if header not set
		if (request.JSON) {
            def wordsToAdd = request.JSON.words
            anagramService.addToDataStore(wordsToAdd)
            render (status: 201, text: 'success')
        }
        else {
            //TODO, what if not JSON?
        }
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
