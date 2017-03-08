package tests;

import global.Convert;
import global.Minibase;
import global.Page;
import global.PageId;

/**
 * Test suite for the bufmgr layer.
 */
class BMTest extends TestDriver {

  /** The display name of the test suite. */
  private static final String TEST_NAME = "buffer manager tests";

  /**
   * Test application entry point; runs all tests.
   */
  public static void main(String argv[]) {

    // create a clean Minibase instance
    BMTest bmt = new BMTest();
    try{
    bmt.create_minibase();
    }
    catch(Exception e)
    {
    	System.out.println(e);
    }

    // run all the test cases
    System.out.println("\n" + "Running " + TEST_NAME + "...");
    boolean status = PASS;
    status &= bmt.test1();
    status &= bmt.test2();
    status &= bmt.test3();

    // display the final results
    System.out.println();
    if (status != PASS) {
      System.out.println("Error(s) encountered during " + TEST_NAME + ".");
    } else {
      System.out.println("All " + TEST_NAME + " completed successfully!");
    }

  } // public static void main (String argv[])

  /**
   * 
   */
  protected boolean test1() {

    System.out.print("\n  Test 1 does a simple test of normal buffer ");
    System.out.print("manager operations:\n");

    // we choose this number to ensure that at least one page
    // will have to be written during this test
    boolean status = PASS;
    int numPages = Minibase.BufferManager.getNumUnpinned() + 1;
    Page pg = new Page();
    PageId pid;
    PageId lastPid;
    PageId firstPid = new PageId();

    System.out.print("  - Allocate a bunch of new pages\n");
    try {
      firstPid = Minibase.BufferManager.newPage(pg, numPages);
    } catch (Exception e) {
      System.err.print("*** Could not allocate " + numPages);
      System.err.print(" new pages in the database.\n");
      e.printStackTrace();
      return false;
    }

    // unpin that first page... to simplify our loop
    try {
      Minibase.BufferManager.unpinPage(firstPid, UNPIN_CLEAN);
    } catch (Exception e) {
      System.err.print("*** Could not unpin the first new page.\n");
      e.printStackTrace();
      status = FAIL;
    }

    System.out.print("  - Write something on each one\n");
    pid = new PageId();
    lastPid = new PageId();

    for (pid.pid = firstPid.pid, lastPid.pid = pid.pid + numPages; status == PASS
        && pid.pid < lastPid.pid; pid.pid = pid.pid + 1) {

      try {
        Minibase.BufferManager.pinPage(pid, pg, PIN_DISKIO);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not pin new page " + pid.pid + "\n");
        e.printStackTrace();
      }

      if (status == PASS) {

        // Copy the page number + 99999 onto each page. It seems
        // unlikely that this bit pattern would show up there by
        // coincidence.
        int data = pid.pid + 99999;
        Convert.setIntValue(data, 0, pg.getData());

        if (status == PASS) {
          try {
            Minibase.BufferManager.unpinPage(pid, UNPIN_DIRTY);
          } catch (Exception e) {
            status = FAIL;
            System.err
                .print("*** Could not unpin dirty page " + pid.pid + "\n");
            e.printStackTrace();
          }
        }
      }
    }

    if (status == PASS)
      System.out.print("  - Read that something back from each one\n"
          + "   (because we're buffering, this is where "
          + "most of the writes happen)\n");

    for (pid.pid = firstPid.pid; status == PASS && pid.pid < lastPid.pid; pid.pid = pid.pid + 1) {

      try {
        Minibase.BufferManager.pinPage(pid, pg, PIN_DISKIO);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not pin page " + pid.pid + "\n");
        e.printStackTrace();
      }

      if (status == PASS) {

        int data = 0;
        data = Convert.getIntValue(0, pg.getData());

        if (status == PASS) {
          if (data != (pid.pid) + 99999) {
            status = FAIL;
            System.err.print("*** Read wrong data back from page " + pid.pid
                + "\n");
          }
        }

        if (status == PASS) {
          try {
            Minibase.BufferManager.unpinPage(pid, UNPIN_CLEAN);
          } catch (Exception e) {
            status = FAIL;
            System.err.print("*** Could not unpin page " + pid.pid + "\n");
            e.printStackTrace();
          }
        }
      }
    }

    if (status == PASS) {
      System.out.print("  - Free the pages again\n");

      for (pid.pid = firstPid.pid; pid.pid < lastPid.pid; pid.pid = pid.pid + 1) {

        try {
          Minibase.BufferManager.freePage(pid);
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** Error freeing page " + pid.pid + "\n");
          e.printStackTrace();
        }
      }
    }

    if (status == PASS)
      System.out.print("  Test 1 completed successfully.\n");

    return status;

  } // protected boolean test1 ()

  /**
   * 
   */
  protected boolean test2() {

    System.out.print("\n  Test 2 exercises some illegal buffer "
        + "manager operations:\n");

    // we choose this number to ensure that pinning
    // this number of buffers should fail
    int numPages = Minibase.BufferManager.getNumUnpinned() + 1;
    Page pg = new Page();
    PageId pid, lastPid;
    PageId firstPid = new PageId();
    boolean status = PASS;

    System.out.print("  - Try to pin more pages than there are frames\n");
    try {
      firstPid = Minibase.BufferManager.newPage(pg, numPages);
    } catch (Exception e) {
      System.err.print("*** Could not allocate " + numPages);
      System.err.print(" new pages in the database.\n");
      e.printStackTrace();
      return false;
    }

    pid = new PageId();
    lastPid = new PageId();

    // first pin enough pages that there is no more room
    for (pid.pid = firstPid.pid + 1, lastPid.pid = firstPid.pid + numPages - 1; status == PASS
        && pid.pid < lastPid.pid; pid.pid = pid.pid + 1) {

      try {
        Minibase.BufferManager.pinPage(pid, pg, PIN_MEMCPY);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not pin new page " + pid.pid + "\n");
        e.printStackTrace();
      }
    }

    // make sure the buffer manager thinks there's no more room
    if (status == PASS && Minibase.BufferManager.getNumUnpinned() != 0) {
      status = FAIL;
      System.err.print("*** The buffer manager thinks it has "
          + Minibase.BufferManager.getNumUnpinned() + " available frames,\n"
          + "    but it should have none.\n");
    }

    // now pin that last page, and make sure it fails
    if (status == PASS) {
      try {
        Minibase.BufferManager.pinPage(lastPid, pg, PIN_MEMCPY);
      } catch (IllegalStateException exc) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL; // what we want
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.print("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    if (status == PASS) {
      try {
        Minibase.BufferManager.pinPage(firstPid, pg, PIN_DISKIO);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not acquire a second pin on a page\n");
        e.printStackTrace();
      }

      if (status == PASS) {
        System.out.print("  - Try to free a doubly-pinned page\n");
        try {
          Minibase.BufferManager.freePage(firstPid);
        } catch (IllegalArgumentException e) {
          System.out.println("  --> Failed as expected \n");
          status = FAIL; // what we want
        } catch (Exception e) {
          e.printStackTrace();
        }

        if (status == PASS) {
          status = FAIL;
          System.err.print("The expected exception was not thrown\n");
        } else {
          status = PASS;
        }
      }

      if (status == PASS) {
        try {
          Minibase.BufferManager.unpinPage(firstPid, UNPIN_CLEAN);
        } catch (Exception e) {
          status = FAIL;
          e.printStackTrace();
        }
      }
    }

    if (status == PASS) {
      System.out.print("  - Try to unpin a page not in the buffer pool\n");
      try {
        Minibase.BufferManager.unpinPage(lastPid, UNPIN_CLEAN);
      } catch (IllegalArgumentException exc) {
        System.out.println("  --> Failed as expected \n");
        status = FAIL; // what we want
      } catch (Exception e) {
        e.printStackTrace();
      }

      if (status == PASS) {
        status = FAIL;
        System.err.print("The expected exception was not thrown\n");
      } else {
        status = PASS;
      }
    }

    for (pid.pid = firstPid.pid; pid.pid <= lastPid.pid; pid.pid = pid.pid + 1) {
      try {
        if (pid.pid != lastPid.pid)
          Minibase.BufferManager.unpinPage(pid, UNPIN_CLEAN);
        Minibase.BufferManager.freePage(pid);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Error freeing page " + pid.pid + "\n");
        e.printStackTrace();
      }
    }

    if (status == PASS)
      System.out.print("  Test 2 completed successfully.\n");

    return status;

  } // protected boolean test2 ()

  /**
   * 
   */
  protected boolean test3() {

    System.out.print("\n  Test 3 exercises some of the internals "
        + "of the buffer manager\n");

    int index;
    int numPages = BUF_SIZE + 10;
    Page pg = new Page();
    PageId pid = new PageId();
    PageId[] pids = new PageId[numPages];
    boolean status = PASS;

    System.out.print("  - Allocate and dirty some new pages, one at "
        + "a time, and leave some pinned\n");

    for (index = 0; status == PASS && index < numPages; ++index) {
      try {
        pid = Minibase.BufferManager.newPage(pg, 1);
      } catch (Exception e) {
        status = FAIL;
        System.err.print("*** Could not allocate new page number " + index + 1
            + "\n");
        e.printStackTrace();
      }

      if (status == PASS)
        pids[index] = pid;

      if (status == PASS) {

        // Copy the page number + 99999 onto each page. It seems
        // unlikely that this bit pattern would show up there by
        // coincidence.
        int data = pid.pid + 99999;

        Convert.setIntValue(data, 0, pg.getData());

        // Leave the page pinned if it equals 12 mod 20. This is a
        // random number based loosely on a bug report.
        if (status == PASS) {
          if (pid.pid % 20 != 12) {
            try {
              Minibase.BufferManager.unpinPage(pid, UNPIN_DIRTY);
            } catch (Exception e) {
              status = FAIL;
              System.err.print("*** Could not unpin dirty page " + pid.pid
                  + "\n");
            }
          }
        }
      }
    }

    if (status == PASS) {
      System.out.print("  - Read the pages\n");

      for (index = 0; status == PASS && index < numPages; ++index) {
        pid = pids[index];
        try {
          Minibase.BufferManager.pinPage(pid, pg, PIN_DISKIO);
        } catch (Exception e) {
          status = FAIL;
          System.err.print("*** Could not pin page " + pid.pid + "\n");
          e.printStackTrace();
        }

        if (status == PASS) {

          int data = 0;
          data = Convert.getIntValue(0, pg.getData());

          if (data != pid.pid + 99999) {
            status = FAIL;
            System.err.print("*** Read wrong data back from page " + pid.pid
                + "\n");
          }
        }

        if (status == PASS) {
          try {
            // might not be dirty
            Minibase.BufferManager.unpinPage(pid, UNPIN_DIRTY);
          } catch (Exception e) {
            status = FAIL;
            System.err.print("*** Could not unpin page " + pid.pid + "\n");
            e.printStackTrace();
          }
        }

        if (status == PASS && (pid.pid % 20 == 12)) {
          try {
            Minibase.BufferManager.unpinPage(pid, UNPIN_DIRTY);
          } catch (Exception e) {
            status = FAIL;
            System.err.print("*** Could not unpin page " + pid.pid + "\n");
            e.printStackTrace();
          }
        }
      }
    }

    if (status == PASS)
      System.out.print("  Test 3 completed successfully.\n");

    return status;

  } // protected boolean test3 ()

} // class BMTest extends TestDriver
