#Buffer manager with the Clock replacement policy#

In this project, we have implemented the buffer manager layer. This layer is responsible for
communicating with the disk space manager and retrieving pages, that are requested by the upper
layer. The buffer manager reads disk pages into a main memory page as needed. The buffer
manager is used by access methods, heap files, and relational operators to read, write, allocate
and de-allocate pages.

A database buffer pool is an array of fixed-sized memory buffers called frames that are
used to hold database pages (also called disk blocks) that have been read from disk into memory.
A page is the unit of transfer between the disk and the buffer pool residing in main memory. Once
a page has been read from disk to the buffer pool, the DBMS software can update information
stored on the page, causing the copy in the buffer pool to be different from the copy on disk. Such
pages are termed “dirty”.

Conceptually, all the frames in the buffer pool are arranged in a circle around the face of a clock. Associated with each frame is a referenced flag. Each time a page in the buffer pool is unpinned, the referenced flag of the corresponding frame is set to true. Whenever we need to choose a frame for replacement, the current frame pointer is advanced using modular arithmetic. This corresponds to the clock hand moving around the face of the clock in a clockwise fashion. The figure above illustrates the execution of the clock algorithm 

##OVERALL STATUS##
In this project we have implemented buffer manager with the Clock replacement policy. We have created a Buffer Pool, Frame Table and Page Map. 

###*Buffer Manager class:*###
The major methods of BufMgr.java has been implemented are as follows: 

**1.	public BufMgr(int numbufs)** 
In this method we are creating frame table frameTab and buffer pool bufpool. Then we have initialized each with the total number of buffers numbufs . Then we have created an object of Clock as replacer and an object of HashMap as pagemap. 
This constructor sets the foundation of the buffer manager. 

**2.	public PageId newPage(Page firstpg, int run_size)**
This method is used to allocate a new page on the disk by invoking allocate_page() method from the DiskManager class. We have created an Exception handling scheme to allocate the page. Once the page is created it is pinned using the pinPage() method.
 If an exception is raised then it deallocates all the invalid pages from the buffer pool, by invoking deallocate_page() method from DiskManager class. Finally the page id of the new page is returned.

**3.	public void freePage(PageId firstid)**
This method deallocates a page from disk after removing it from the buffer (if present). This method initially check if the page is still pinned, in which case it raises an exception IllegalArgumentException and displays “Page is pinned and cannot be removed” message.
 If the page is not pinned, we proceed to remove the page from the buffer. While doing so, we set the pin count of the page to 0, make the dirty bit as FALSE, change the page ID to the constant INVALID_PAGEID. The page is removed from the HashMap. Then the status of the page is set to *AVAILABLE* by invoking freePage() method from Clock class. 
Finally the page is deallocated from the buffer pool, by invoking deallocate_page() method from DiskManager class.

**4.	public void pinPage(PageId pageno, Page page, boolean skipRead)**
This method is used to bring a page into the buffer from the disk. This is the most important routine for a buffer manager. If the page is already in the buffer, then we increment its pin count. However if the page is not present, we need to find a buffer frame which can be replaced with a new page.
 We have achieved this by creating a pickVictim() method in clock class, which has been invoked here. If the dirty bit of the page being replaced is set then the page is written to the disk using write_page() of DiskManager class.  
Once the new page is read, its pin count is set to 1. It is included in the HashMap and its status is updated to *PINNED*. 

**5.	public void unpinPage(PageId pageno,  Boolean dirty)**
This method unpins a page when it is no longer needed by the user who pinned it. The unpinning of a page does not result in immediate write of the page to the disk.
 The pin count of the page is decreased by 1 and its status is updated to *REFERENCED*. The unpinned pages continue to exist in the buffer until the space occupied by them is needed to bring another page into the buffer.

**6.	public void flushPage(PageId pageno)**
This method is used to write the page to the disk which has its dirty bit set to 1. We have used write_page() method of DiskManager class to perform the write operation.

**7.	public void flushAllPages()**
This method is used to write all valid pages to the disk. In this method we iteratively find out the pages whose dirty bit is set to 1 and is immediately written to the disk by invoking flushPage() method explained above.

**8.	public int getNumBuffers()** 
This method returns the length of the buffer pool which indicates the number of buffers.

**9.	public int getNumUnpinned()**
This method returns total number of unpinned pages in the buffer pool.

The above methods successfully implement the buffer manager. No we will discuss about the Clock class which extends Replacer class.

###*Clock class*###
 In the clock class we have implemented the following methods.

**1.	public Clock(BufMgr bufmgr)**
This constructor helps in initializing the buffer frames. In this method the state of each page frame is set to *AVAILABLE*. The value of the clock head has been initialized to -1. 

**2.	public void pinPage(FrameDesc fdesc)**
This method updates the state of the frame to *PINNED* which will notify the replacer that the page has been pinned. 

**3.	public void unpinPage(FrameDesc fdesc)**
This method updates the state of the frame to *REFERENCED* which will notify the replacer that the page has been unpinned.


**4.	public int pickVictim()**

- This is the most critical part of the clock replacement policy. This method identifies the frame which can be used to replace with a new frame.

- The current frame, frametab[head]  is considered for replacement. If the frame is not chosen for replacement, head is incremented and the next frame is considered. We keep repeating this process until some frame is chosen. 

- The pinPage() method in buffer manager class often invoked this method when pinning a new page. If the state of the current frame is *PINNED*, then it is not a candidate for replacement and current is incremented. 

- If the current frame is in *REFERENCED* state, the clock algorithm updates the state of the frame to *AVAILABLE*.  This way a recently referenced page is less likely to be replaced. 
If the state of the current frame is *AVAILABLE* then the page in it is chosen for replacement.
If all frames are pinned in some sweep of the clock hand (that is, the value of current is incremented until it repeats), this means that no page in the buffer pool is a replacement candidate.


###*Conclusion*###

This project tested our understanding of the lower layers of the database architecture. We clearly understood the concept of Buffer Management and application of clock replacement policy in it. We also gained the hands on knowledge of the *Minibase* system. Initially we struggled with the class packages and methods defined in bufmgr package of *Minibase*, but overall it was a great learning experience. We hope to receive such challenging and interesting projects further in the course.

*Skeleton Code provided by _Prof. Sharma Chakravarthy_ (UT Arlington)*

*Team members: Rajat Dhanuka & Anvit Bhimsain Joshi* 
