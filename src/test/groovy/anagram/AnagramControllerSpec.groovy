package anagram

import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 */
@TestFor(AnagramController)
class AnagramControllerSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "test addWords method"() {
		given:
            AnagramService mockAnagramService = Mock()
            controller.anagramService = mockAnagramService
        when:
			request.json = '{"words" : ["read", "dear", "dare"]}'
			request.method = 'POST'
			controller.addWords()
        then:
		    1 * mockAnagramService.addToDataStore(!null)
			response.text == 'created'
    }
}
