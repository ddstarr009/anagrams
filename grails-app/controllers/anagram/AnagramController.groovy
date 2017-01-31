package anagram


import grails.rest.*
import grails.converters.*
import grails.transaction.*
import static org.springframework.http.HttpStatus.*
import static org.springframework.http.HttpMethod.*

class AnagramController {
    def anagramService // using Springs DI by convention here
	static responseFormats = ['json', 'xml']
	
    def index() { 
		def testMap = ['test' : 'pants']
		render testMap as JSON
    }

    // should be for GET /bleh/${word}
    //def show() { }

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
		//println request.JSON
		//println params
    }

    // should be for DELETE
    //def delete() { }
    def exception(Exception exception) {
        logException exception
        render (status: 400, text: exception?.message)
    }

	/** Log exception */
    private void logException(Exception exception) {
        log.error "Exception occurred. ${exception?.message}", exception
    }
}
