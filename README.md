# BackendlessDataCollection

This is an implementation of the Java Collection interface enabling to retrieve and iterate over a collection of objects stored in a Backendless data table.</p>

Interface methods returning data are mapped to various Backendless APIs.</p>

The Iterator returned by the implementation lets you access either all objects from the data table or a subset determined by a where clause. Additionally, the implementation can work with data streams.</p>

---

The collection has _**two modes of operation**_:
  - _**persisted**_ - all retrieved objects are saved locally to enable faster access in future iterations. The persisted data is shared between all iterators returned by the collection. To enable this mode use the "preservedData" parameter.
  - _**transient**_ - every iterator returned by the collection works with a fresh data collection returned from the server.
 
Also be sure you properly mapped your custom type with\
`Backendless.Data.mapTableToClass( String tableName, Class<T> entityClass )`

---

