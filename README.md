# BackendlessDataCollection

This is an implementation of the Java Collection interface enabling to retrieve and iterate over a collection of objects stored in a Backendless data table.</p>

Interface methods returning data are mapped to various Backendless APIs.</p>

The Iterator returned by the implementation lets you access either all objects from the data table or a subset determined by a where clause. Additionally, the implementation can work with data streams.</p>

---

The collection has _**two modes of operation**_:
  - _**persisted**_ - all retrieved objects are saved locally to enable faster access in future iterations. The persisted data is shared between all iterators returned by the collection. To enable this mode use the `preserveIteratedData` parameter.
  - _**transient**_ - every iterator returned by the collection works with a fresh data collection returned from the server.

The collection is not thread safe.

Also be sure you properly mapped your custom type with\
`Backendless.Data.mapTableToClass( String tableName, Class<T> entityClass )`

---


# User Guide

## Create collection
```java
   BackendlessDataCollection<Order> orders;
1. orders = new BackendlessDataCollection<>( Order.class );
2. orders = new BackendlessDataCollection<>( Order.class, "title = 'phone'" );
3. orders = new BackendlessDataCollection<>( Order.class, true );
4. orders = new BackendlessDataCollection<>( Order.class, true, "title = 'phone'" );
```

**Description:**

1. Create **ordinary collection** for table _**order**_ which reflect all records from it.
- the total size of objects (table rows) is retrieved on object creation;
- you can iterate through the entire collection;
- every iteration will perfrom calls to the Backendless server;
- all iterators are independent, so you may create the numbers of it and use it simultaneously;
- if the iterator reached the end (wasn't interrupted) the actual size is refreshed automatically;
- all `contains`, `add` and `remove` operations directly perform calls to Backendless server;
- method `invalidateState()` forcibly updates collection real size;
- method `isPersisted()` always returns _true_;
- methods `isLoaded()`, `getPersistedSize()`, `populate()` will throw exception, because they are intended only for persisted collection;

2. Create **collection as a slice** of data for table _**order**_. Will reflect only a subset of data which satisfy argument `slice` (in or case it `title = 'phone'`).\
Main features are the same as in point (1).
- the total size of objects satisfied the _slice_ is retrieved on object creation;
- you can iterate only through the subset of objects;
- all `contains`, `add` and `remove` operations directly perform calls to Backendless server and would be discarded if the object ;

3. Create **persisted collection** for table _**order**_ (`preserveIteratedData` parameter). Some operations would perform locally (without api calls to the server) and thus drastically reduce perform time.\
Main features are the same as in point (1).
- only first iteration will perfrom calls to the Backendless server, all subsequent interations will  be done locally without any request;
- if the iterator was interrupted, only part of the data would be saved locally and next time iterator firstly will iterate over local data and then (if need) beging make calls to the server;
- method `isPersisted()` always returns _true_;
- when all data from table saved locally, `isLoaded()` returns _true_;
- method `getPersistedSize()` returns the size of locally saved data, it will be equal to `size()` if `isLoaded() == true`
- method `invalidateState()` forcibly updates collection real size and clear all locally saved data, so the next iteration will make requests to the server again;
- method `populate()` forcibly download all data from table, if `isLoaded() == true` it do nothing;

4. Create **persisted collection as a slice** for table _**order**_.\
Combine features from point features are the same as in point (2) and (3).

