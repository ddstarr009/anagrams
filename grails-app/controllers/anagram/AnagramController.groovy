package anagram


import grails.rest.*
import grails.converters.*
import grails.transaction.*
import redis.clients.jedis.Jedis
import static org.springframework.http.HttpStatus.*
import static org.springframework.http.HttpMethod.*

class AnagramController {
	def redisService
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
            // multiple commands will only use a single connection instance by using withRedis
            redisService.withRedis { Jedis redis ->
                //redis.set("bleh", "meh")
                // sort word chars alphabetically
                for (int i = 0 ; i < wordsToAdd.size() ; i++) {
                    def word = wordsToAdd.get(i)
                    char[] wordCharArray = word.toCharArray()
                    Arrays.sort(wordCharArray)
                    String sortedWord = new String(wordCharArray)
                    redis.sadd(sortedWord, word)

                    println 'hey word: ' + word
                    println 'hey sorted word: ' + sortedWord
                }
            }

        }
		//println request.JSON
		//println params
		render (status: 201, text: 'created test')
    }

    // should be for DELETE
    //def delete() { }
}
