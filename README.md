# BackendlessDataCollection

This is an implementation of the Java Collection interface enabling to retrieve and iterate over a collection of objects stored in a Backendless data table.</p>

Interface methods returning data are mapped to various Backendless APIs.</p>

The Iterator returned by the implementation lets you access either all objects from the data table or a subset determined by a where clause. Additionally, the implementation can work with data streams.</p>

---

The collection has _**two modes of operation**_:
  - **persisted mode** - all retrieved objects are saved locally to enable faster access in future iterations. The persisted data is shared between all iterators returned by the collection. To enable this mode use the `preserveIteratedData` parameter.
  - **transient mode** - every iterator returned by the collection works with a fresh data collection returned from the server.

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

## Description

1. Create **ordinary collection** for table _**order**_ which reflect all records from it. By default it is created in **transient mode**, thus no data will safe locally.
- the total size of objects (table rows) is retrieved on object creation;
- you can iterate through the entire collection;
- every iteration will perfrom calls to the Backendless server;
- all iterators are independent, so you may create the numbers of it and use it simultaneously;
- if the iterator reached the end (wasn't interrupted) the actual collection size is refreshed automatically;
- all `contains`, `add` and `remove` operations directly perform calls to Backendless server;
- method `invalidateState()` forcibly updates collection real size;
- method `isPersisted()` always returns _true_;
- methods `isLoaded()`, `getPersistedSize()`, `populate()` will throw exception, because they are intended only for **persisted** mode;

2. Create **collection as a slice** of data for table _**order**_. Will reflect only a subset of data which satisfy argument `slice` (in or case it `title = 'phone'`).\
Main features are the same as in point (1).
- the total size of objects satisfied the _slice_ is retrieved on object creation;
- you can iterate only through the subset of objects;
- all `contains`, `add` and `remove` operations directly perform calls to Backendless server and would be discarded if the object ;

3. Create **persisted collection** for table _**order**_ (`preserveIteratedData` parameter). Some operations would perform locally (without api calls to the server) and thus drastically reduce perform time.\
Main features are the same as in point (1).
- collection is lazy, so the data will be loaded only during iteration over it;
- only first iteration will perfrom calls to the Backendless server, all subsequent interations will  be done locally without any request;
- if the iterator was interrupted, only part of the data would be saved locally and next time iterator firstly will iterate over local data and then (if need) beging make calls to the server;
- method `isPersisted()` always returns _true_;
- when all data from table saved locally, `isLoaded()` returns _true_;
- method `getPersistedSize()` returns the size of locally saved data, it will be equal to `size()` if `isLoaded() == true`
- method `invalidateState()` forcibly updates collection real size and clear all locally saved data, so the next iteration will make requests to the server again;
- method `populate()` forcibly download all data from the table (so-called greedy initialization), if `isLoaded() == true` it do nothing;

4. Create **persisted collection as a slice** for table _**order**_.\
Combine features from points (2) and (3).


## Methods' features (differences from the regular java collection) and special methods

#### `size()`
Returns the current size of collection which reflect the row size in the underlying table. If the table was changed on the server side, the local become irrelevant. For this case you may use `invalidateState()` method or perform iteration over collection (after iteration size is updated automatically). Never makes api call to Backendless.

#### `isEmpty()`
Never makes api call to Backendless. Related to `size()` method.

#### `getSlice()`
Returns where clause for the current collection or `null` if it was created without slice.

#### `isPersisted()`
Returns **true** if the collection was created with parameter `preserveIteratedData == true`, and **false** otherwise.

#### `populate()`
Only for **persisted mode**.\
Forcibly populates current collection from the Backendless data table (greedy initialization). If `isLoaded() == true`, do nothing. Under the hood it just iterate over remote table.

#### `isLoaded()`
Only for **persisted mode**.\
Returns **true** if the data was retrieved from Backendless table in a full (after invocation `populate()` method or full iteration).

#### `getPersistedSize()`
Only for **persisted mode**.\
Returns the size of inner store. It may differ from `size()` return value if the iteration over collectin was interrupted.

#### `invalidateState()`
For **transient mode** forcibly updates collection real size. For **persisted mode** in addition clear all locally saved data, so the next iteration will make requests to the server again.

#### `clear()`
Remove all data from the underlying table (if the collection was created without `slice`) or remove the subset of data specified in the condition (with `slice`). Then the method calls `invalidateState()`.

#### `getById(String objectId)`
Returns object by its `objectId`. Takes into account slice (where clause).
If this collection is **persisted** and fully loaded, than no api-calls will be performed.

#### `toArray()`
It is always greedy operation. Calling this method will retrive all data from the table.

#### `equals()`
Takes into account only 'entityType' and 'slice', that were set during collection creation.

#### `add()`, `addAll()`, `remove()`, `removeAll()`, `retainAll()`
Always perfrom api calls to Backendless. Even in **persisted** mode it is necessary to synchronize local state and remote table.

#### `contains()`, `containsAll()`
If this collection is **persisted** and fully loaded, than no api-calls will be performed.


## Examples

#### for-each
```java
    for( Order o : orders )
      System.out.println( o.getOrderDate() );
```

#### iterator
```java
    Iterator<Order> orderIterator = orders.iterator();
    while( orderIterator.hasNext() )
    {
      Order o = orderIterator.next();
      System.out.println( o.getObjectId() + ", " + o.getTitle() );
    }
```

#### for-each with premature break
```java
    for( Order o : orders )
    {
      System.out.println( o.getTitle() );
      if (o.getTitle().equals( "ticket" ))
        break;
    }
```

#### stream
```java
    List<Set> orderTitles = orders.stream()
        .map( Order::getTitle )
        .collect( Collectors.toSet() );
```

#### convert to array
```java
    Order[] orderArray = orders.toArray();
    String allOrders = Arrays.toString(orderArray);
    System.out.println( allOrders );
```
