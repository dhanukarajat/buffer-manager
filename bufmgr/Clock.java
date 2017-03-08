/**
 *  CSE 5331     : DBMS Models and implementation
 *  Project 1    : Buffer Management with Clock Replacement Policy
 *  Team Members : Anvit Bhimsain Joshi (1001163195) and Rajat Dhanuka (1001214104)
 */

package bufmgr;

/**
 * The "Clock" replacement policy.
 */
 
class Clock extends Replacer {

  // Frame State Constants
 
  protected static final int AVAILABLE = 10;
  protected static final int REFERENCED = 11;
  protected static final int PINNED = 12;

  /** Clock head; required for the default clock algorithm. */
  protected int head;
  int numberOfBuffers;

  /**
   * Constructs a clock replacer.
   */
   
  public Clock(BufMgr bufmgr) {
	  
    super(bufmgr);
	numberOfBuffers = bufmgr.getNumBuffers(); // Retrieves the total number of buffers

    // Initialize the frame states to AVAILABLE
    for (int i = 0; i < frametab.length; i++) {
      frametab[i].state = AVAILABLE;
    }

    // Initialize the clock head
    head = -1;

  } // public Clock(BufMgr bufmgr)

  /**
   * Notifies the replacer of a new page.
   */
   
  public void newPage(FrameDesc fdesc) {
    // no need to update frame state
  }

  /**
   * Notifies the replacer of a free page.
   */
   
  public void freePage(FrameDesc fdesc) {
	  
    fdesc.state = AVAILABLE;
	
  }

  /**
   * Notifies the replacer of a pined page.
   */
  public void pinPage(FrameDesc fdesc) {
	  
	fdesc.state = PINNED;
	
  }

  /**
   * Notifies the replacer of an unpinned page.
   */
   
  public void unpinPage(FrameDesc fdesc) {
	  
	  if (fdesc.pincnt == 0)
		  fdesc.state = REFERENCED;
	
  }

  /**
   * Selects the best frame to use for pinning a new page.
   * 
   * @return victim frame number, or -1 if none available
   */
   
  public int pickVictim() {

    int i=0;
	int findVictim = 1;
	
	while (findVictim == 1){
		
		head = (head+1) % numberOfBuffers;
		
		if (i > 2 * numberOfBuffers)
			return -1; // no buffer frame is free
		
		if (frametab[head].state == REFERENCED)
			frametab[head].state = AVAILABLE;
		
		else if (frametab[head].state == AVAILABLE)
			findVictim = 0; // found the victim frame
		
		i++;	// continue to next buffer frame
	}
	
	return head;

  } // public int pick_victim()

} // class Clock extends Replacer
