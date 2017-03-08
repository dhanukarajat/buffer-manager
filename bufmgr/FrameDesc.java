/**
 *  CSE 5331     : DBMS Models and implementation
 *  Project 1    : Buffer Management with Clock Replacement Policy
 *  Team Members : Anvit Bhimsain Joshi (1001163195) and Rajat Dhanuka (1001214104)
 */

package bufmgr;

import global.PageId;

/**
 * A frame descriptor; contains info about each page in the buffer pool.
 */
 
class FrameDesc {

  /** Index in the buffer pool. */
  public int index;
  
  /** Identifies the frame's page. */
  public PageId pageno;

  /** The frame's pin count. */
  public int pincnt;

  /** The frame's dirty status. */
  public boolean dirty;

  /** Generic state used by replacers. */
  public int state;

  /**
   * Default constructor; empty frame.
   */
   
  public FrameDesc(int index) {
    this.index = index;
    pageno = new PageId();
    pincnt = 0;
    dirty = false;
    state = 0;
  }

  
  /** Manage Pin Count */
  
  public int getPinCount() {
	  return this.pincnt;
	  	  /** Retrieves the pin count of the current page */
  }
  
  public void setPinCount(int a) {
	  this.pincnt = a;
	  	   /** Sets the pin count of the current page, typically adds or subtracts 1 from current pin count */
  }
  
  /** Manage Dirty Bit */
  
  public boolean getDirty() {
	  return this.dirty;
		  /** Retrieves whether the current page is modified or not */
  }
   
} // class FrameDesc
