### Hi there!  Try out this snappy anagram API!  Have fun :)

## Instructions to run the app locally
 - App was developed with Grails version 3.2.4, Groovy version 2.4.7, and JVM version 1.7.0_80

 - Install git if needed

 - git clone git@github.com:ddstarr009/anagrams.git
    - you will want to be a collaborator and have your public ssh key added to the repo settings

 - Install Redis 3.2.x, excellent instructions here: https://redis.io/topics/quickstart
    - by default it should run on port 6379, which we want

 - Install Java
    - download and install jdk 1.7 or later and set your JAVA_HOME

 - Install Grails
    - Download Grails(if on Windows) or use sdkman(easiest method for Bash platforms) 
    - sdkman instructions
        - curl -s get.sdkman.io | bash # you may need to install unzip and zip for the curl result to work properly
        - source "$HOME/.sdkman/bin/sdkman-init.sh"
        - sdk install grails # this will install the latest stable Grails version and latest Groovy
        - run "grails -version" and you should see your Grails, Groovy, and JVM versions.  If not, something went wrong
 - Running Grails
    - from your project root, enter "grails run-app" on the command line.  This will run your app on localhost:8080


// GET anagrams for a word
        "/api/v1/anagrams/$word"(controller: 'anagram', action:'findAnagrams', method: 'GET')

        // GET that takes a set of words and returns whether or not they are all anagrams of each other
        "/api/v1/anagrams/checker"(controller: 'anagram', action:'anagramChecker', method: 'GET')

        // GET that returns a count of words in the corpus and min/max/median/average word length
        "/api/v1/words/stats"(controller: 'anagram', action:'wordsStats', method: 'GET')

        // GET that identifies words with the most anagrams
        "/api/v1/anagrams/most"(controller: 'anagram', action:'mostAnagrams', method: 'GET')

        // GET that returns all anagram groups of size >= x
        "/api/v1/anagrams/groups/min/$minSize"(controller: 'anagram', action:'anagramGroups', method: 'GET')

		// POST for adding words to dictionary
        "/api/v1/words"(controller: 'anagram', action:'addWords', method:'POST')

        // DELETE a single word from data store
        "/api/v1/words/$word"(controller: 'anagram', action:'deleteWord', method:'DELETE')
        
        // DELETE all contents of data store
        "/api/v1/words"(controller: 'anagram', action:'deleteAllWords', method:'DELETE')

        // DELETE a word and all of its anagrams
        "/api/v1/anagrams/$word"(controller: 'anagram', action:'deleteAnagramFamily', method:'DELETE')

