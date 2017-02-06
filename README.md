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


## API docs

 - GET anagrams for a given word
    - /api/v1/anagrams/:word 
    - Optional URL Params(they can be used together)
        - limit
            - example usage: /api/v1/anagrams/<someword>?limit=5
                - this will limit your result to 5 words or less
        - proper
            - example usage: /api/v1/anagrams/<someword>?proper=false
                - this will filter out any proper nouns based on capitalization of the first letter
                - with no proper param passed, the default is to include proper nouns

 - GET that takes a set of words and returns whether or not they are all anagrams of each other
    - /api/v1/anagrams/checker
    - Required URL param
        - words
            - example usage: /api/v1/anagrams/checker?words=silent,listen

 - GET that returns a count of words in the corpus and min/max/median/average word length
    - /api/v1/words/stats

 - GET that identifies words with the most anagrams
    - /api/v1/anagrams/most

 - GET that returns all anagram groups of size >= x
    - /api/v1/anagrams/groups/min/:minSize
    - example usage: /api/v1/anagrams/groups/min/7

 - POST for adding words to the dictionary.  Takes JSON array of words
    - /api/v1/words
    - POST data must adhere to example JSON structure below 
    - example req: curl -i -X POST -d '{ "words": ["read", "dear"] }' localhost:8080/api/v1/words 
        - this content-type application/x-www-form-urlencoded is supported as well as application/json

 - DELETE a single word from data store
    - /api/v1/words/:word
        
 - DELETE all contents of data store
    - /api/v1/words

 - DELETE a word and all of its anagrams
    - /api/v1/anagrams/:word

