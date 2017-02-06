I am a read me placeholder

 14 - `POST /words.json`: Takes a JSON array of English-language words and adds them to the corpus (data store).
 15 - `GET /anagrams/:word.json`:
 16   - Returns a JSON array of English-language words that are anagrams of the word passed in the URL.
 17   - This endpoint should support an optional query param that indicates the maximum number of results to return.
 18 - `DELETE /words/:word.json`: Deletes a single word from the data store.
 19 - `DELETE /words.json`: Deletes all contents of the data store.

 31 -  DELETE a word and all of its anagrams
 32         "/api/anagrams/family/$word"(controller: 'anagram', action:'deleteAnagramFamily', method:'DELETE')


 34 -  GET  Endpoint that takes a set of words and returns whether or not they are all anagrams of each other "/api/anagrams/$word"(controller: 'anagram', action:'show', method: 'GET')

 Instructions to run the app locally:
 - Install git
 - git clone git@github.com:ddstarr009/anagrams.git
    - you will need to be a collaborator or have your public ssh key added to the repo

  - App was developed with Grails version 3.2.4, Groovy version 2.4.7, and JVM version 1.7.0_80

 Install Redis 3.2.x, excellent instructions here: https://redis.io/topics/quickstart
 - by default it should run on port 6379, which we want

 Install Java
 - download and install jdk 1.7 or later and set your JAVA_HOME

 Install Grails
 - Download Grails(if on Windows) or use sdkman(easiest method for Bash platforms) 
 - sdkman instructions
   - curl -s get.sdkman.io | bash # you may need to install unzip and zip for the curl result to work properly
   - source "$HOME/.sdkman/bin/sdkman-init.sh"
   - sdk install grails # this will install the latest stable Grails version and latest Groovy
   - run "grails -version" and you should see your Grails, Groovy, and JVM versions.  If not, something went wrong

