/**
 *  CSE 5331     : DBMS Models and implementation
 *  Project 1    : Buffer Management with Clock Replacement Policy
 *  Team Members : Anvit Bhimsain Joshi (1001163195) and Rajat Dhanuka (1001214104)
 */
 
package bufmgr;

import global.GlobalConst;
import global.Page;
import global.PageId;
import java.util.HashMap;
import global.Minibase;

/**
 * <h3>Minibase Buffer Manager</h3>
 * The buffer manager reads disk pages into a main memory page as needed. The
 * collection of main memory pages (called frames) used by the buffer manager
 * for this purpose is called the buffer pool. This is just an array of Page
 * objects. The buffer manager is used by access methods, heap files, and
 * relational operators to read, write, allocate, and de-allocate pages.
 */
public class BufMgr implements GlobalConst {

    /** Actual pool of pages (can be viewed as an array of byte arrays). */
    protected Page[] bufpool;

    /** Array of descriptors, each containing the pin count, dirty status, etc\
	. */
    protected FrameDesc[] frametab;

    /** Maps current page numbers to frames; used for efficient lookups. */
    protected HashMap<Integer, FrameDesc> pagemap;

    /** The replacement policy to use. */
    protected Replacer replacer;
//-------------------------------------------------------------



  /**
   * Constructs a buffer mamanger with the given settings.
   * 
   * @param numbufs number of buffers in the buffer pool
   */
  public BufMgr(int numbufs) {
    
	frametab = new FrameDesc[numbufs];						// Creating Frame Table
	bufpool = new Page[numbufs];							// Creating Buffer Pool
	
	// Initialize each frametab and bufferpool
	for (int i=0; i<numbufs; i++) {
		frametab[i] = new FrameDesc(i);
		bufpool[i] = new Page();
	}
  
  	replacer = new Clock(this);								// Creating Clock object
	pagemap = new HashMap<Integer, FrameDesc>(numbufs);		// Creating Page Map
  
  }

  /**
   * Allocates a set of new pages, and pins the first one in an appropriate
   * frame in the buffer pool.
   * 
   * @param firstpg holds the contents of the first page
   * @param run_size number of pages to allocate
   * @return page id of the first new page
   * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned
   * @throws IllegalStateException if all pages are pinned (i.e. pool exceeded)
   */

  public PageId newPage(Page firstpg, int run_size) {
    
	PageId page_id = Minibase.DiskManager.allocate_page(run_size);  // Adds new page to buffer pool
	
	try{
		pinPage(page_id, firstpg, true); // Invoke pinPage method to pin a new page
	}
	
	catch(Exception e) {
		//
		for(int i=0; i<run_size; i++){
			page_id.pid += i;
			Minibase.DiskManager.deallocate_page(page_id); // Removes all illegal pages from buffer pool
		}
		return null;
	}
	
	return page_id; // Returns page ID of new page
	
  }

  /**
   * Deallocates a single page from disk, freeing it from the pool if needed.
   * 
   * @param pageno identifies the page to remove
   * @throws IllegalArgumentException if the page is pinned
   */
  public void freePage(PageId pageno) {
    
	FrameDesc frameIndex = pagemap.get(pageno.pid); // Sets the frameIndex variable to the page Id which is to be freed
	
	if(frameIndex != null){
		
		if(frameIndex.getPinCount() > 0) // check if the page is still pinned
			throw new IllegalArgumentException("page is pinned, can not be removed.");
		else{
			frameIndex.pageno.pid = INVALID_PAGEID;
			pagemap.remove(pageno.pid);
			replacer.freePage(frameIndex); // update frame state to AVAILABLE
		} // End of else part, for valid pages which have pincount of zero.
		
		Minibase.DiskManager.deallocate_page(pageno);	// Removes all illegal pages from buffer pool
		
	} 
  } // end of void freePage method

  /**
   * Pins a disk page into the buffer pool. If the page is already pinned, this
   * simply increments the pin count. Otherwise, this selects another page in
   * the pool to replace, flushing it to disk if dirty.
   * 
   * @param pageno identifies the page to pin
   * @param page holds contents of the page, either an input or output param
   * @param skipRead PIN_MEMCPY (replace in pool); PIN_DISKIO (read the page in)
   * @throws IllegalArgumentException if PIN_MEMCPY and the page is pinned
   * @throws IllegalStateException if all pages are pinned (i.e. pool exceeded)
   */
  public void pinPage(PageId pageno, Page page, boolean skipRead) {
    
	FrameDesc frameIndex = pagemap.get(pageno.pid); //Put the page Id to be pinned in frameIndex variable
	int start = 0; 
	
	/** for pages which are already in the buffer pool */
	if (frameIndex != null){
		 if (skipRead)
			throw new IllegalArgumentException("invalid argument");
		
		 else{
			frameIndex.setPinCount(frameIndex.getPinCount()+1); // Increase pin count of page by 1
			page.setPage(bufpool[frameIndex.index]);
			replacer.pinPage(frameIndex); // Update frame state to PINNED
			return;
		 } // The page is pinned
	}
	
	/** for pages which are not in the buffer pool. Find the victim to remove it from buffer.
	* get a new page and pin that new page
	*/
	else{
		start = replacer.pickVictim(); // Gets the id of victim page
		
		if (start == -1) // pickVictim() doesn't sent a valid frame, all buffers are pinned
			throw new IllegalStateException("buffer is full, all pages are pinned");
		
		frameIndex = frametab[start];
		
		if (frameIndex.pageno.pid != -1) {
			
			pagemap.remove(frameIndex.pageno.pid); // Removing the page reference from hashmap
			
			// Checking if the page to be removed is dirty, if it is, then write it to disk.
			 if (frameIndex.getDirty())
			 Minibase.DiskManager.write_page(frameIndex.pageno, bufpool[start]);
		}
		
		if (skipRead)
			bufpool[start].copyPage(page);
		
		else
			Minibase.DiskManager.read_page(pageno, bufpool[start]); // Reading page from the disk into bufferpool
		
		/** initialize the new page */
		
	    frameIndex.pincnt = 1; // Setting pincount to 1
		page.setPage(bufpool[start]);
		pagemap.put(pageno.pid, frameIndex); // Including the page in hashmap
		frameIndex.pageno.pid = pageno.pid;
		replacer.pinPage(frameIndex); // update frame state to PINNED 
	}
  }

  /**
   * Unpins a disk page from the buffer pool, decreasing its pin count.
   * 
   * @param pageno identifies the page to unpin
   * @param dirty UNPIN_DIRTY if the page was modified, UNPIN_CLEAN otherrwise
   * @throws IllegalArgumentException if the page is not present or not pinned
   */
  public void unpinPage(PageId pageno, boolean dirty) {
    
	FrameDesc frameIndex = pagemap.get(pageno.pid); //Put the page Id to be unpinned in frameIndex variable
	
	if (frameIndex == null) // Checking for invalid pages
		throw new IllegalArgumentException("Page is not present");
	
	else {
		if (frameIndex.getPinCount() > 0 ){
			frameIndex.setPinCount(frameIndex.getPinCount()-1); // Decrease pin count of page by 1
			frameIndex.dirty = dirty;
			replacer.unpinPage(frameIndex); // Updating frame state to REFERENCED
			return;
		}
	}
  }

  /**
   * Immediately writes a page in the buffer pool to disk, if dirty.
   */
  public void flushPage(PageId pageno) {
    
	for (int i=0; i<frametab.length; i++){
		if ((pageno == null || frametab[i].pageno.pid == pageno.pid) && frametab[i].dirty){
			Minibase.DiskManager.write_page(frametab[i].pageno, bufpool[i]);
		}
	}
  }

  /**
   * Immediately writes all dirty pages in the buffer pool to disk.
   */
  public void flushAllPages() {
	  
    for (int i=0; i<frametab.length; i++)
		flushPage(frametab[i].pageno); // Iteratively invokes flushPage method to check and write back to disk
  }

  /**
   * Gets the total number of buffer frames.
   */
  public int getNumBuffers() {
    
	return bufpool.length;
  }

  /**
   * Gets the total number of unpinned buffer frames.
   */
  public int getNumUnpinned() {
	  
    int unpinCount=0;
	
	for (int i=0; i<bufpool.length;i++){
		if (frametab[i].getPinCount() == 0)
			unpinCount += 1;
	}
	return unpinCount;
  }

} // public class BufMgr implements GlobalConst
