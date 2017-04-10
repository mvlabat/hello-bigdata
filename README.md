# hello-bigdata

## Running
```
sbt run
```

## Prerequisites
This application uses facebook Graph API. Currently it can be run only
on `http://mvlabat.host:8080/` domain address. This requires to edit `/etc/hosts` a little bit.

This application expects that mongodb database is already created and
collections are predefined. Look at `/src/main/scala/Database.scala`.

Example of **users** collection structure:
```
> db.users.find().pretty()
{
        "_id" : ObjectId("582660f74e9ab2d84de2b782"),
        "email" : "someone@gmail.com",
        "password" : "123"
}
```

Example of **ids** collection structure:
```
> db.ids.find().pretty()
{
     "_id" : ObjectId("582651bc4e9ab2d84de2b77f"),
     "email" : "someone@gmail.com",
     "id" : "toset"
}
```

## Things implemented
* User registration (check `http://mvlabat.host:8080/register`)
* Repeating password validation
* User login (check `http://mvlabat.host:8080/login`)
* Facebook authorization using Graph API (check `http://mvlabat.host:8080/oauth`)
* Scheduling email sending (only scheduling, no emails are actually sent, configuring smtp is a TODO task)
* Saving results to database (only user id, see below)

## Things not implemented
* Scrapping all the user posts and messages
    * (It appeared impossible because Facebook requires app validation for this permission: https://developers.facebook.com/docs/facebook-login/permissions#reference-user_posts)
    * But for demonstration purpose the application retrieves facebook user id
