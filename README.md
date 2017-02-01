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

