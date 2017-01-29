package anagram


import grails.rest.*
import grails.converters.*
import grails.transaction.*
import static org.springframework.http.HttpStatus.*
import static org.springframework.http.HttpMethod.*

class AnagramController {
	def redisService
    def anagramService // using Springs DI by convention here
	static responseFormats = ['json', 'xml']
	
    def index() { 
        //log.info "hello there index action"
        println "Hello David"
		def testMap = ['test' : 'pants']
		render testMap as JSON
    }

    // should be for GET /bleh/${word}
    //def show() { }

    // for POST
    def save() { 

        // TODO, deal with content type if header not set
		if (request.JSON) {
            // TODO, move to some kind of data store service class

            def wordsToAdd = request.JSON.words
            anagramService.addToDataStore(wordsToAdd)

        }
		//println request.JSON
		//println params
		render (status: 201, text: 'created test')
    }

    // should be for DELETE
    //def delete() { }
}
