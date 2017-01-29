package anagram


import grails.rest.*
import grails.converters.*
import grails.transaction.*
import static org.springframework.http.HttpStatus.*
import static org.springframework.http.HttpMethod.*

class AnagramController {
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
		
        // /words.json, takes json array and adds to data store, prob redis
		//curl -i -X POST -d '{ "words": ["read", "dear", "dare"] }' http://localhost:3000/words.json
    }

    // should be for DELETE
    //def delete() { }
}
