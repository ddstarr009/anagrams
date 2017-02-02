package anagram

class BootStrap {
    def grailsApplication
    def anagramService

    def init = { servletContext ->
        def file = grailsApplication.classLoader.getResource("dictionary.txt")
        def lines = file.readLines()
        anagramService.addToDataStore(lines)
    }

    def destroy = {
    }
}
