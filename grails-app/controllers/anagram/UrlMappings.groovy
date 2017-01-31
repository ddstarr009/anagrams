package anagram

class UrlMappings {

    static mappings = {
        delete "/$controller/$id(.$format)?"(action:"delete")
        get "/$controller(.$format)?"(action:"index")
        get "/$controller/$id(.$format)?"(action:"show")
        post "/$controller(.$format)?"(action:"save")
        put "/$controller/$id(.$format)?"(action:"update")
        patch "/$controller/$id(.$format)?"(action:"patch")

        "/"(controller: 'application', action:'index')
        "500"(view: '/error')
        "404"(view: '/notFound')

        // anagram mappings

        // GET anagrams for a word
        "/api/anagrams/$word"(controller: 'anagram', action:'show', method: 'GET')

		// POST for adding words to dictionary
        "/api/words"(controller: 'anagram', action:'save', method:'POST')

        // DELETE a single word from data store
        "/api/words/$word"(controller: 'anagram', action:'delete', method:'DELETE')
    }
}
