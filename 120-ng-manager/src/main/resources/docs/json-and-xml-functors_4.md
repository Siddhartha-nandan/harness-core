## list()

* **Syntax:** `list(string, string)`
* **Description:** Returns list object.
* **Parameters:** literal string, string (typically, `httpResponseBody`). Using the `list().get()` method returns items from the list.

**Example:**

Here is the JSON we will query:


```json
{  
  "books": [{  
      "isbn": "9781593275846",  
      "title": "Eloquent JavaScript, Second Edition",  
      "subtitle": "A Modern Introduction to Programming",  
      "author": "Marijn Haverbeke",  
      "published": "2014-12-14T00:00:00.000Z",  
      "publisher": "No Starch Press",  
      "pages": "472",  
      "description": "JavaScript lies at the heart of almost every modern web application, from social apps to the newest browser-based games. Though simple for beginners to pick up and play with, JavaScript is a flexible, complex language that you can use to build full-scale applications."  
    },  
    {  
      "isbn": "9781449331818",  
      "title": "Learning JavaScript Design Patterns",  
      "subtitle": "A JavaScript and jQuery Developer's Guide",  
      "author": "Addy Osmani",  
      "published": "2012-07-01T00:00:00.000Z",  
      "publisher": "O'Reilly Media",  
      "pages": "254",  
      "description": "With Learning JavaScript Design Patterns, you'll learn how to write beautiful, structured, and maintainable JavaScript by applying classical and modern design patterns to the language. If you want to keep your code efficient, more manageable, and up-to-date with the latest best practices, this book is for you."  
    },  
    {  
      "isbn": "9781449365035",  
      "title": "Speaking JavaScript",  
      "subtitle": "An In-Depth Guide for Programmers",  
      "author": "Axel Rauschmayer",  
      "published": "2014-02-01T00:00:00.000Z",  
      "publisher": "O'Reilly Media",  
      "pages": "460",  
      "description": "Like it or not, JavaScript is everywhere these days-from browser to server to mobile-and now you, too, need to learn the language or dive deeper than you have. This concise book guides you into and through JavaScript, written by a veteran programmer who once found himself in the same position."  
    },  
    {  
      "isbn": "9781491950296",  
      "title": "Programming JavaScript Applications",  
      "subtitle": "Robust Web Architecture with Node, HTML5, and Modern JS Libraries",  
      "author": "Eric Elliott",  
      "published": "2014-07-01T00:00:00.000Z",  
      "publisher": "O'Reilly Media",  
      "pages": "254",  
      "description": "Take advantage of JavaScript's power to build robust web-scale or enterprise applications that are easy to extend and maintain. By applying the design patterns outlined in this practical book, experienced JavaScript developers will learn how to write flexible and resilient code that's easier-yes, easier-to work with as your code base grows."  
    }  
  ]  
}
```
You can find this example at <https://raw.githubusercontent.com/wings-software/harness-docs/main/functors/books.json>.

Here is the query using the `list()` method to select `pages` from the 3rd book:


```json
<+json.list("books", httpResponseBody).get(2).pages>
```
Since the JSON array starts at 0, `get(2)` returns `pages` from the third list item (`"pages": "460"`).

We can add the `list()` method to an HTTP step and output it using the variable **list**:

![](./static/json-and-xml-functors-11.png)

When this HTTP step is executed, in its **Output** tab, you can see the HTTP response in **HTTP Response Body** and the list item in the **Output Variables**:

![](./static/json-and-xml-functors-12.png)
