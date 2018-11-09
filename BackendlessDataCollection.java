
import com.backendless.Backendless;
import com.backendless.IDataStore;
import com.backendless.persistence.DataQueryBuilder;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * <p>Plain proxy object for convenient work with Backendless Data service.
 * <br>
 * <br>
 * <p>It map ordinary java Collection's methods to the api calls to Backendless.
 * <p>It support iteration over all data in your table (on the server side), making the process seamless from the first object and till the end of the table.
 * <p>It have ability to be mapped only on particular subset from all data in your table by using Backendless where clause.
 * <p>It supports standard java api for iterations and working with data streams.
 * <br>
 * <br>
 * <p>It has two operation modes:
 * <ul>
 * <li>persisted -- all iterated objects saved locally for the fast access in further iterations (use <b>{@code preservedData}</b> parameter); the data shared between all iterators;</li>
 * <li>transient -- none of the objects, obtained during the iteration, are saved, so every iterator produce api calls to Backendless server.</li>
 * </ul>
 *
 * @param <T> the type of your entity. Be sure it properly mapped with {@code Backendless.Data.mapTableToClass( String tableName, Class<T> entityClass )}
 */
public class BackendlessDataCollection<T extends BackendlessDataCollection.Identifiable<T>> implements Collection<T>
{
  public static interface Identifiable<T>
  {
    String getObjectId();
  }


  private Class<T> entityType;
  private IDataStore<T> iDataStore;
  private String slice;
  private LinkedHashMap<String, T> preservedData; // if null, the mode is 'transient'
  //private IdentityHashMap
  private int size;
  private boolean isLoaded = false;

  public BackendlessDataCollection( Class<T> entityType )
  {
    this( entityType, false );
  }

  public BackendlessDataCollection( Class<T> entityType, String slice )
  {
    this( entityType, false, slice );
  }

  public BackendlessDataCollection( Class<T> entityType, boolean preserveIteratedData )
  {
    this( entityType, preserveIteratedData, "" );
  }

  public BackendlessDataCollection( Class<T> entityType, boolean preserveIteratedData, String slice )
  {
    this.entityType = entityType;
    this.slice = (slice == null) ? "" : slice;
    this.iDataStore = Backendless.Data.of( this.entityType );
    if( preserveIteratedData )
      this.preservedData = new LinkedHashMap<>();

    this.size = getRealSize();
  }

  public String getSlice()
  {
    return slice;
  }

  public boolean isPersisted()
  {
    return this.preservedData != null;
  }

  public int getPersistedSize()
  {
    if( this.preservedData != null )
      return this.preservedData.size();

    return -1;
  }

  public boolean isLoaded()
  {
    return this.isLoaded;
  }

  public void invalidateState()
  {
    if( this.preservedData != null )
    {
      this.preservedData.clear();
      this.isLoaded = false;
    }

    this.size = getRealSize();
  }

  /**
   * // TODO: recursively iterate and fill up internal cache
   */
  public void populate()
  {

  }

  private int getRealSize()
  {
    return this.iDataStore.getObjectCount( DataQueryBuilder.create().setWhereClause( this.slice ) );
  }

  private void checkObject( Object o )
  {
    if( this.entityType != o.getClass() )
      throw new IllegalArgumentException( o.getClass() + " is not a type objects of which are contained in this collection." );

    String objectId = ((T) o).getObjectId();
    if( objectId == null )
      throw new IllegalArgumentException( "'objectId' is null." );
  }

  private String getQuery( T obj )
  {
    String query = "objectId='" + obj.getObjectId() + "'";
    query = (this.slice.isEmpty()) ? query : this.slice + " and " + query;
    return query;
  }

  private String getQuery( Collection<T> objs )
  {
    StringBuilder sb = new StringBuilder( "objectId in (" );
    objs.stream().map( T::getObjectId ).forEach( obj -> sb.append( '\'' ).append( obj ).append( '\'' ).append( ',' ) );
    sb.replace( sb.length() - 1, sb.length(), ")" );

    String query = sb.toString();
    query = (this.slice.isEmpty()) ? query : this.slice + " and " + query;
    return query;
  }

  @Override
  public Iterator<T> iterator()
  {
    return new BackendlessDataCollectionIterator();
  }

  @Override
  public int size()
  {
    return this.size;
  }

  @Override
  public boolean remove( Object o )
  {
    this.checkObject( o );

    boolean result = false;

    if( this.preservedData != null )
      result = this.preservedData.remove( ((T) o).getObjectId() ) != null;

    result |= this.iDataStore.remove( this.getQuery( (T) o ) ) != 0;

    return result;
  }

  @Override
  public boolean removeAll( Collection<?> c )
  {
    c.forEach( this::checkObject );

    boolean result = false;

    if( this.preservedData != null )
    {
      for( T entity : (Collection<T>) c )
        result = this.preservedData.remove( entity.getObjectId() ) != null;
    }

    result |= this.iDataStore.remove( this.getQuery( (Collection<T>) c ) ) != 0;

    return result;
  }

  @Override
  public boolean isEmpty()
  {
    return this.size == 0;
  }

  @Override
  public boolean contains( Object o )
  {
    this.checkObject( o );

    boolean result = false;

    if( this.preservedData != null )
      result = this.preservedData.containsKey( ((T) o).getObjectId() );

    DataQueryBuilder queryBuilder = DataQueryBuilder.create().setWhereClause( this.getQuery( (T) o ) );
    result |= this.iDataStore.getObjectCount( queryBuilder ) != 0;

    return result;
  }

  // TODO: -------------------------

  @Override
  public T[] toArray()
  {
    T[] array = (T[]) Array.newInstance( entityType, size);
    return array;
  }

  @Override
  public <T1> T1[] toArray( T1[] a )
  {
    return (T1[]) this.toArray();
  }

  @Override
  public boolean add( T t )
  {
    return false;
  }

  @Override
  public boolean containsAll( Collection<?> c )
  {
    return false;
  }

  @Override
  public boolean addAll( Collection<? extends T> c )
  {
    return false;
  }

  @Override
  public boolean retainAll( Collection<?> c )
  {
    return false;
  }

  // TODO: end -------------------------

  @Override
  public void clear()
  {
    this.iDataStore.remove( this.slice );
    invalidateState();
  }

  @Override
  public boolean equals( Object o )
  {
    if( this == o )
      return true;
    if( !(o instanceof BackendlessDataCollection) )
      return false;
    BackendlessDataCollection<?> that = (BackendlessDataCollection<?>) o;
    return Objects.equals( entityType, that.entityType ) && Objects.equals( slice, that.slice );
  }

  @Override
  public int hashCode()
  {
    return Objects.hash( entityType, slice );
  }


  public class BackendlessDataCollectionIterator implements Iterator<T>
  {
    private static final int pageSize = 2;

    private final Object syncKey = new Object();
    private DataQueryBuilder queryBuilder;
    private int currentPosition;
    private List<T> currentPageData;
    private List<T> nextPageData;

    private BackendlessDataCollectionIterator()
    {
      this.currentPosition = 0;
      this.queryBuilder = DataQueryBuilder.create().setWhereClause( BackendlessDataCollection.this.slice ).setPageSize( pageSize );

      if (BackendlessDataCollection.this.isLoaded)
        return;

      if (BackendlessDataCollection.this.preservedData != null)
      {
/*
        int moveSize = (BackendlessDataCollection.this.preservedData.size() < pageSize )?BackendlessDataCollection.this.preservedData.size():pageSize;
        currentPageData = new ArrayList<>();

        for( int i = 0; i < moveSize; i++)
        {

        }

        if (BackendlessDataCollection.this.preservedData.size() < pageSize)
        {
          currentPageData = new ArrayList<>( BackendlessDataCollection.this.preservedData.values() );
        }
        else
        {
          for( int i = 0; i< )
          {

          }
        }
*/

      }
      else
      {
        this.currentPageData = BackendlessDataCollection.this.iDataStore.find( this.queryBuilder );
        this.nextPageData = BackendlessDataCollection.this.iDataStore.find( this.queryBuilder.prepareNextPage() );
      }
    }

    @Override
    public boolean hasNext()
    {
      return ((currentPageData != null && currentPosition % pageSize < currentPageData.size())
               || (nextPageData != null && !nextPageData.isEmpty())
             );
    }

    @Override
    public T next()
    {
      // TODO: retrieve the object from remote or local storage

      if( this.currentPageData == null )
        throw new NoSuchElementException();

      int indexOnPage = currentPosition++ % pageSize;

      T result = null;

      if( indexOnPage < currentPageData.size() )
        result = currentPageData.get( indexOnPage );

      if( indexOnPage == currentPageData.size() - 1 )
        getNextPage();

      // TODO: put the object to local storage if BackendlessDataCollection.this.preservedData != null

      return result;
    }

    private void getNextPage()
    {
      if( currentPageData == null || nextPageData == null || nextPageData.isEmpty() )
        currentPageData = nextPageData = null;
      else
      {
        currentPageData = nextPageData;

        if( currentPageData.size() < pageSize )
          this.nextPageData = null;
        else
        {
          this.nextPageData = BackendlessDataCollection.this.iDataStore.find( this.queryBuilder.prepareNextPage() );

          // TODO: add to local storage

          if( this.nextPageData.size() < pageSize )
            BackendlessDataCollection.this.size = currentPosition + this.nextPageData.size();
        }
      }
    }


  }
}
