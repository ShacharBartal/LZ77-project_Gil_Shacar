/* Submitted by: 
 * Shachar Bartal 305262016
 * Gil Nevo 310021654 */

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class LZ77 {
	
	/*
	 * lz77 compresses using tuples.
	 * the tuple will be as follows: (d,l,c), d is how far back to go, l
	 * is how many characters to copy, c is the next character after that.
	 */

	private int sliding_window; // the sliding_window
	private int tmp_d; // variable for finding d.
	private int d; // how much going back.
	private int tmp_l; // variable for finding l.	
	private int l; // length of bytes to copy.
	private char c; // the char we will put at each iteration of compression

	private int index_of_compressed_content_bytes_to_output_file = 0;	// index for appointing
	//bytes to compressed_content_bytes_to_output_file variable

	private final int max_sliding_window;// sliding window max size.

	private final int bits_of_max_sliding_window; // sliding window
													// max size in bits.

	private final int look_a_head_buffer; // size of look a head buffer

	private final int bits_of_look_a_head_buffer;// size of look a head buffer
													// in bits.
	private byte[] content_file_as_bytes; // array of all the bytes of the file.

	
	private byte[] compressed_content_bytes_to_output_file;// array of all the 
	//compressed bytes sent to output file.
	
	/* upgraded compress definitions of global variables */

	private boolean upgrade;// gets true if we are going to save the upgrade - just at
	// compress at specific iterate of k.

	private boolean was_first_change;// will be of each new letter we will read
	// (at loop), will be true after first use at upgrade because of saving the changes if we will not use
	// specific upgrade after that time

	private int number_of_changes;// how much changes at each new letter we will
									// read (at loop). counter

	/*constructor - gets bits to use for window as a parameter, needs to be  between 1-7*/
	LZ77(int bits_of_max_sliding_window) {
		
		this.max_sliding_window = (int) Math.pow(2,(double) bits_of_max_sliding_window) - 1; //max_sliding_window,
		//is 2 in the power of the input bits of window from the user.
		
		this.look_a_head_buffer = (int) Math.pow(2,(double) 8-bits_of_max_sliding_window) - 1;//look_a_head_buffer,
		//is 2 in the power of the bits of 8-bits of window from the user.
		
		this.bits_of_look_a_head_buffer = 8-bits_of_max_sliding_window;
		
		this.bits_of_max_sliding_window = bits_of_max_sliding_window;

		// default values to global variables:
		sliding_window = tmp_d = d = tmp_l = l = index_of_compressed_content_bytes_to_output_file = 0;

	}

	/*this method generates a text file that is optimal for our upgraded LZ77.
	 * it makes sure there is no consecutive "errors", and also is pretty small.
	 * so the indices are no more than 1 byte. */
	public void Generate_String(String output_file_path,int size) {
		
		StringBuilder str = new StringBuilder();
		
		str.append("ab"); //making sure file will start with 'ab'.

		double probabilty;
		
		boolean error = false;

		for (int i = 0; i < size-2; i++) {
			probabilty = Math.random();
			if ((probabilty > 0.05) || (error == false)) {
				str.append('a');
				error = true;
			} else {
				str.append((char) (33 + (Math.random() * 113)));
				error = false;
			}
			probabilty = Math.random();
			if ((probabilty > 0.05) || (error == false)) {
				str.append('b');
				error = true;
			} else {
				str.append((char) (33 + (Math.random() * 113)));
				error = false;
			}
		}
		
		String str_ = str.toString();
		
		byte[] generated_str = str_.getBytes(); //byte array 
		//we are sending to output file.
		
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(
					output_file_path);
			fileOutputStream.write(generated_str);
			fileOutputStream.close();

		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Writing The File.");
			e1.printStackTrace();
		}
	}

	/*our implementation of classic LZ77.*/
	public void Compress(String input_file_path, String output_file_path) {

		File input_file = new File(input_file_path); // the input as file

		content_file_as_bytes = new byte[(int) input_file.length()];

		try {
			FileInputStream fileInputStream = new FileInputStream(input_file);
			fileInputStream.read(content_file_as_bytes); // reading all the file into
			// content_file_as_bytes.
			fileInputStream.close();
															

		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Reading The File.");
			e1.printStackTrace();
		}

		compressed_content_bytes_to_output_file = new byte[(int) input_file.length() * 6];

		/*loop that goes all through bytes of input file.*/
		for (int j = 0; j < content_file_as_bytes.length; j++) {
			
			/*
			 * the tuple will be as follows: (d,l,c), d is how far back to go, l
			 * is how many characters to copy, c is the next character after that.
			 */

			c = (char) content_file_as_bytes[j]; //for now c gets the first byte.
			
			sliding_window = j;
			if (sliding_window > max_sliding_window) //sliding window starts at 0 and grows until it reaches max.
				sliding_window = max_sliding_window;

			/*in this loop we are looking for the optimal d that will get us max l*/
			for (int k = 0; k < sliding_window; k++) {
				tmp_l = 0;
				tmp_d = sliding_window - k;
				int step_forward = 0;//step forward to use when we find a match.

				/*while the char in our window matches to the current char go forward.*/
				while ((content_file_as_bytes[j + step_forward] == content_file_as_bytes[j- tmp_d + step_forward])) {
					tmp_l++;
					step_forward++;
					if ((j + step_forward >= content_file_as_bytes.length)|| (step_forward >= look_a_head_buffer))
						break;
				}

				/*choosing max l.*/
				if (tmp_l > l) {
					l = tmp_l;
					d = tmp_d;
					if (j + step_forward + 1 <= content_file_as_bytes.length)
						c = (char) content_file_as_bytes[j + step_forward];
					else
						c = ' ';
				}

			}

			/*sending to a method that writes tuple to file.*/
			AddTo_compressed_content_bytes_to_output_file(
					compressed_content_bytes_to_output_file, d, l, c,
					index_of_compressed_content_bytes_to_output_file);
			
		/*Incrementing index by 2. */
			index_of_compressed_content_bytes_to_output_file = index_of_compressed_content_bytes_to_output_file + 2;
			
			j = j + l; //j is now incremented by l that we copied from window.
			l = 0;
			d = 0;
		}
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(
					output_file_path);
			fileOutputStream.write(compressed_content_bytes_to_output_file, 0,
					index_of_compressed_content_bytes_to_output_file);
			fileOutputStream.close();

		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Writing The File.");
			e1.printStackTrace();
		}

	}

	
	public void CompressWithUpgrade(String input_file_path, String output_file_path) {
		
		/* initializing the local variables*/
		
		 int[] indexes_of_changes;	// here we keep all the indices of
			//the letters we are replacing.
		 
		byte[] letters_to_save;	//here we keep all the letters
		//that were changed.
		
		boolean use_the_upgrade; // gets value true if we finished the k iteration and we are going
		// to use the upgrade
		
		File input_file = new File(input_file_path);	// the input
		content_file_as_bytes = new byte[(int) input_file.length()];	 
		compressed_content_bytes_to_output_file = new byte[(int) input_file.length() * 6];
		indexes_of_changes = new int[(int) input_file.length()];
		letters_to_save = new byte[(int) input_file.length()];
		int indexesAtFinito=0; // counter for indexes_of_changes and letters_to_save_bytes.
		int final_number_of_changes; // counter of all the changes at a specific
		// iteration of j (when reading a new char).
		
		
		// reading the input:
				try {
					FileInputStream fileInputStream = new FileInputStream(input_file);
					fileInputStream.read(content_file_as_bytes); // reading all the file into content_file_as_bytes.
					fileInputStream.close();

				} catch (FileNotFoundException e) {
					System.out.println("File Not Found.");
					e.printStackTrace();
				} catch (IOException e1) {
					System.out.println("Error Reading The File.");
					e1.printStackTrace();
				}

				// j loop : each j will be a new letter we read
				for (int j = 0; j < content_file_as_bytes.length; j++) {

					// default values to the variables that we need at the loop:
					use_the_upgrade = was_first_change = false;
					number_of_changes = final_number_of_changes = 0;
					c = (char) content_file_as_bytes[j];

					// defining local variables of loop:

					/*
					 * we have 3 sets of arrays - temporary and final(save) for each set:
					 * indexes, old chars and new bytes after each upgrade. temporary -
					 * will change each new upgrade save - will change if we see that
					 * last upgrade was optimal the size will be the size of
					 * look_a_head_buffer because we want the size of the arrays will be
					 * a little bit more then the maximum of change we can do at same
					 * compress
					 */

					byte[] temp_save_char_after_switch = new byte[look_a_head_buffer]; // all the bytes (after changes)
					
					int[] temp_index_of_upgrade_ = new int[look_a_head_buffer]; // we save here all
																				// the indices.
					
					byte[] temp_save_old_char = new byte[look_a_head_buffer]; //we save here all the
																			// bytes (before changes).
					byte[] save_old_char = new byte[look_a_head_buffer];
					byte[] save_char_after_twist = new byte[look_a_head_buffer];
					int[] save_index_of_upgrade = new int[look_a_head_buffer];

					// maybe we will need to change c (the variable) also, so we will
					// use the next variables:
					int tempOfFinalIndex = 0; // index of c.
					byte tempOfFinalOldChar = 0; // c before change
					byte tempOfFinalCharAfterTwist = 0; // c after change
					boolean changeTheExtra = false; // gets value true if we need to use the change.

					// initializing final (save) arrays:
					for (int i = 0; i < look_a_head_buffer; i++) {
						save_char_after_twist[i] = 0;
						save_old_char[i] = 0;
						save_index_of_upgrade[i] = 0;
					}

					// defining sliding window using j.
					sliding_window = j;
					if (sliding_window > max_sliding_window)//sliding window starts at 0 and grows until it reaches max.
						sliding_window = max_sliding_window;

					/* after all initializations, we are reading index [j], it's variable c, and we
					start the loop of sliding window:*/

					for (int k = 0; k < sliding_window; k++) {

						// initializing values to local variables, for checking optimal copying from window.
						number_of_changes = 0;
						tmp_l = 0;
						tmp_d = sliding_window - k;
						upgrade = false;
						int step_forward = 0; //step forward to use when we find a match.

						// initializing values to the temporary arrays:
						for (int i = 0; i < look_a_head_buffer; i++) {
							temp_save_char_after_switch[i] = 0;
							temp_save_old_char[i] = 0;
							temp_index_of_upgrade_[i] = 0;
						}
						
						/*checking if the char in our window doesn't match to the current char.*/

						if (!(content_file_as_bytes[j + step_forward] == content_file_as_bytes[j
								- tmp_d + step_forward])) { // if its not equal
							if (j + step_forward + 1 < content_file_as_bytes.length) { // checking that
													// we are not in the last letter of the file.
								
								/*
								 * if we are here it means that the char in our window
								 * doesn't match to the current char
								 * so now we need to check if 'upgrade' will be useful:
								 * we will do this by checking the next letter: so we need to check if the
								 * char we will copy:
								 * (char) content_file_as_bytes[j -tmp_d + step_forward+1] 
								 * is equal to the char we are
								 * reading (char) content_file_as_bytes[j +step_forward+1]
								 */
						
					
						if ((content_file_as_bytes[j + step_forward+1] == content_file_as_bytes[j - tmp_d + step_forward+1])) {
							// if its equal, we send it to check upgrade.
							
							byte charBeforeChange = content_file_as_bytes[j + step_forward];  // we save the char we read.
							
							// we send it to check copy and save the values:
							checkIfUpgrade(tmp_l, step_forward, j, temp_index_of_upgrade_, temp_save_old_char, temp_save_char_after_switch,  save_old_char, save_index_of_upgrade, save_char_after_twist);
							putNumbersAtUpgrade(temp_index_of_upgrade_, temp_save_old_char, temp_save_char_after_switch, save_old_char, save_index_of_upgrade, save_char_after_twist, charBeforeChange, j, step_forward);
							
						}
					}	
				}
				
				/*  important ! if boolean upgrade is true, it just means that we may use it, we still didn't compress
				*	anything.
				*	if upgrade was true, until the end of the iteration, there is a change at content-file, so the next while-loop uses it.
				*/		
				
				while ((content_file_as_bytes[j + step_forward] == content_file_as_bytes[j
						- tmp_d + step_forward])) {
					
					// while the byte we read is equal to the byte we copy
					
					
					tmp_l++;						   // adding one to tmp_l that may change l 
					step_forward++;						// adding one to step_forward to check our next char we want to copy
					
					// if we passed the look_a_head_buffer or finished reading the bytes
					if ((j + step_forward >= content_file_as_bytes.length)  
							|| (step_forward >= look_a_head_buffer))
						break;
					
					/*
						we are checking if the byte we read: content_file_as_bytes[j + step_forward],
						is equal to the byte we try to copy: content_file_as_bytes[j- tmp_d + step_forward]
					*/
					
					if (!(content_file_as_bytes[j + step_forward] == content_file_as_bytes[j- tmp_d + step_forward])) { // if its not equal		
						 if (j + step_forward+1 < content_file_as_bytes.length) { // if we are not in the last byte of file
							 
							 /* if we are here, it means that The next byte we read is content_file_as_bytes[j+step_forward],
							  *  and it's unequal to content_file_as_bytes[j- tmp_d + step_forward],
							  *  so now we need to check about the upgrade, 
							  *  the next step letter is:   copy: content_file_as_bytes[j - tmp_d + step_forward+1], 
							  *  							read: content_file_as_bytes[j + step_forward+1]    
							  */
						
							if ((content_file_as_bytes[j + step_forward+1] == content_file_as_bytes[j - tmp_d + step_forward+1])) {								
								 // if we are here its mean that the next step of bytes are equals, so we need to check upgrade
								
								byte charBeforeChange = content_file_as_bytes[j + step_forward];  // save the char we read
								
								//send to check if it will be good to use upgrade:
								checkIfUpgrade(tmp_l, step_forward, j, temp_index_of_upgrade_, temp_save_old_char, temp_save_char_after_switch,  save_old_char, save_index_of_upgrade, save_char_after_twist);
								// save values
								putNumbersAtUpgrade(temp_index_of_upgrade_, temp_save_old_char, temp_save_char_after_switch, save_old_char, save_index_of_upgrade, save_char_after_twist, charBeforeChange, j, step_forward);							
							}
						}	
					}
					
				} // end while
				
				if ( (tmp_l > l) || ( (tmp_l==l) && (!upgrade) && (tmp_l>0) ) || ((tmp_l==l) && (upgrade) && (tmp_l>0) && (final_number_of_changes>number_of_changes) ) ){

					/* if we found optimal compression at that iteration: tmp_l bigger then l.
						--	or tmp_l equal to l but not using the upgrade - we prefer to use the usual lz_77 if it is the same 
								l, because the upgrade is writing more bytes to compressed file
					 	--	or we using the upgrade but with less changes - we prefer to use compression with much less changes because each change adds more size to compressed file
					 	*/
					if (j+step_forward<content_file_as_bytes.length) { // if we still did not finish to read
						// we save the values of the next char, that is going to be the c variable
						tempOfFinalIndex = j+step_forward;
						tempOfFinalOldChar = content_file_as_bytes[j+step_forward];
						tempOfFinalCharAfterTwist =  content_file_as_bytes[j - tmp_d + step_forward];
					}
					
					if (j + step_forward + 1 <= content_file_as_bytes.length)    {
						if ( content_file_as_bytes[j - tmp_d + step_forward]  != content_file_as_bytes[j + step_forward]) {
							// we are here if we need to use the upgrade on c - to change the next byte to the next byte we would like to copy 
							
							changeTheExtra=true; // boolean to indicate we used the upgrade on c , so we will save all 
													//the values at the array of the upgrade at the end of compress
							c = (char) content_file_as_bytes[j - tmp_d + step_forward] ; // we save c as the next byte we would like to copy	
						}
						
						else { // else c will get the next char like usual lz_77
							c = (char) content_file_as_bytes[j + step_forward];	
						}				
					}	
					
					else	// if we finished reading the file
						c = ' ';
					
					if (upgrade) {
						// if we found optimal compression and upgrade was in use:
						
						final_number_of_changes = number_of_changes; // we save the number of changes
						use_the_upgrade=true; 						// we turn on use_the_upgrade so the program will know to use it at the end of the loop

						for (int i=0; i<number_of_changes; i++) {
							// we save the values that were at temp-arrays to save-arrays:
							save_old_char[i] = temp_save_old_char[i];
							save_index_of_upgrade[i] = temp_index_of_upgrade_[i];	
							save_char_after_twist[i] = temp_save_char_after_switch[i];
							
							// because we still did not finis hchecking optimal compression, we are back to content-file the way it was before last changes
							content_file_as_bytes[temp_index_of_upgrade_[i]] = save_old_char[i];  
						}
					}
					
					else { // if we found optimal compression and upgrade was not in use:
						use_the_upgrade = false; // we turn off use_the_upgrade so the program will not use it at the end

						if (was_first_change) { // if we changed already someting at content-file, we will return it now:
							for (int i=0; i<number_of_changes; i++) {							
								content_file_as_bytes[temp_index_of_upgrade_[i]] = temp_save_old_char[i];
							}
						}
					}
				
					l = tmp_l;
					d = tmp_d;	
					// and for now we have optimal (d, l ,c) 
				}
				
				
				
				else if (was_first_change) { // if we finished iteration, we changed something but at the end we did not use it, we will return it now:
					for (int i=0; i<number_of_changes; i++) {							
						content_file_as_bytes[temp_index_of_upgrade_[i]] = temp_save_old_char[i];
					}
				}
				
				if (step_forward >= look_a_head_buffer) { // if step_forward used all the look_a_head_buffer we will not continue loop
					break;
				}	
			} // end k (loop of sliding window)
			
			if (use_the_upgrade) { 
				/* 
				 * if the optimal compression we found was with upgrade, we will change the 
				 *  content_file as upgrade and save values at the final array of upgrade 
				*/ 
					
				for (int i=0; i<final_number_of_changes; i++) {
					content_file_as_bytes[save_index_of_upgrade[i]] =save_char_after_twist[i];	
				}
				
				for (int i=0; i<final_number_of_changes; i++) {
					
					indexes_of_changes[indexesAtFinito] = save_index_of_upgrade[i];
					letters_to_save[indexesAtFinito] = save_old_char[i];
					
					/*
					 * we save upgrade: at index [indexes_of_changes[indexesAtFinito]] its now: save_char_after_twist[i]
					 * and the decompressed will change it at the specific index to letters_to_save[indexesAtFinito]. 
					 */
					indexesAtFinito++;
				}
			}
			
			if (changeTheExtra) { 
				/* if we used upgrade also for c, we will change the content_file as upgrade 
				 * and save values at the final array of upgrade 
				 */
				content_file_as_bytes[tempOfFinalIndex] =tempOfFinalCharAfterTwist;	
				indexes_of_changes[indexesAtFinito] = tempOfFinalIndex;
				letters_to_save[indexesAtFinito] = tempOfFinalOldChar;

				/* we save upgrade: at index [indexes_of_changes[indexesAtFinito]]
				 * its now: tempOfFinalCharAfterTwist, we will change later to tempOfFinalOldChar 
				 * at specific index
				 */
				indexesAtFinito++;
				changeTheExtra=false; // we turn off the changeTheExtra
			}

			// we send the values to compress
			AddTo_compressed_content_bytes_to_output_file(
					compressed_content_bytes_to_output_file, d, l, c,
					index_of_compressed_content_bytes_to_output_file);
			index_of_compressed_content_bytes_to_output_file = index_of_compressed_content_bytes_to_output_file + 2;
			
			// and we will continue to the next iteration with refreshed values:
			j = j + l;
			l = 0;
			d = 0;

		} // end of j loop - the chars we read
		
		try {
			// after we finished reading the file, write the compressed file
			FileOutputStream fileOutputStream = new FileOutputStream(output_file_path);
			DataOutputStream out = new DataOutputStream(fileOutputStream);
			
			/*writing to beginning of file the indices and letters that were changed.*/
			
			/*we check the size of file, if the file is under 256 bytes for example,
			 *we can use 1 byte only for each index.*/
			if(content_file_as_bytes.length <= 256){
		    out.writeInt(indexesAtFinito*2);//writing for the decompressor how much of beginning of file
		    // is related to the letters to fix and nor regular compressed data.
		    
		    out.writeByte(1);//writing if we are using 1,2,3 or 4 bytes for indices.
		    
			}
			else if(content_file_as_bytes.length <= 65536){
				out.writeInt(indexesAtFinito*3);//writing for the decompressor how much of beginning of file
			    // is related to the letters to fix and nor regular compressed data
				
			    out.writeByte(2);//writing if we are using 1,2,3 or 4 bytes for indices.
			}
			else if(content_file_as_bytes.length <= 16777216){
				out.writeInt(indexesAtFinito*4);//writing for the decompressor how much of beginning of file
			    // is related to the letters to fix and nor regular compressed data.
				
			    out.writeByte(3);//writing if we are using 1,2,3 or 4 bytes for indices.
			}
			else{
				out.writeInt(indexesAtFinito*5);//writing for the decompressor how much of beginning of file
			    // is related to the letters to fix and nor regular compressed data.
				
			    out.writeByte(4);//writing if we are using 1,2,3 or 4 bytes for indices.
			}
			
			
			for (int i = 0; i < indexesAtFinito; i++) {//writing all the indices and letters that we wll fix later.
				
				
				if(content_file_as_bytes.length <= 256) //writing index.
				    out.writeByte(indexes_of_changes[i]);
				
				else if(content_file_as_bytes.length <= 65536)//writing index.
					out.writeShort(indexes_of_changes[i]);
				
				else if(content_file_as_bytes.length <= 16777216){//writing index.
					byte[] b=ByteBuffer.allocate(4).putInt(indexes_of_changes[i]).array();
					out.write(b, 1, 3);
				}
				else                                     //writing index.
					out.writeInt(indexes_of_changes[i]);
				
				out.writeByte(letters_to_save[i]);      //writing letter.
			}
			fileOutputStream.write(compressed_content_bytes_to_output_file, 0,
					index_of_compressed_content_bytes_to_output_file);
			fileOutputStream.close();

		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Writing The File.");
			e1.printStackTrace();
		}
	}  // end of method.
	
	private void checkIfUpgrade(int l_, int step_forward_, int j_, int[] temp_index_of_upgrade_, byte[] temp_save_old_char, 
			byte[] temp_save_char_after_switch, byte[] save_old_char, int[] save_index_of_upgrade, byte[] save_char_after_twist) {

		/*
		 * this function is called when there is an option to upgrade,
		 * we will change the content text and check if it is optimal at the end of current iteration. if it is, we will keep the changes
		 * of the text, else - we will change back the original text
		 */
		
		// saving the variables we are going to change.
		byte char_after_switch_of_iterate = content_file_as_bytes[j_- tmp_d + step_forward_];
		content_file_as_bytes[j_ + step_forward_] = char_after_switch_of_iterate; // change the content
		
			/* so we sent the byte content_file_as_bytes[j_+step_forward_] to check if we will switch it to
			 *  char_after_switch_of_iterate, at that specific index of the source input it will be better then to do it as 
			 *  usual_lzz
			 * So for temp, at index[temp_index_of_iterate] we get char_after_switch_of_iterate  
			 */

		do {
			// becuase we know that the change of content-file makes one successful copy for sure, we use do-while loop for checking the next bytes
			step_forward_++;
			
				if ((j_ + step_forward_+1 >= content_file_as_bytes.length)|| (step_forward_ >= look_a_head_buffer)) {
					// if we finished read the file or finished look_a_buffer
					break;
				}
				
				/* 	the next lines will do the same as at the compressWithUpgrade method, that check if we need to use
				  	upgrade again - at recursion at the upgrade that we are in it
				  */
				
				if (!(content_file_as_bytes[j_ + step_forward_] == content_file_as_bytes[j_- tmp_d + step_forward_])) {
					if (j_ + step_forward_+1 < content_file_as_bytes.length) {  
						if ((content_file_as_bytes[j_ + step_forward_+1] == content_file_as_bytes[j_ - tmp_d + step_forward_+1])) {
							
							// saving values..
							int temp_step = step_forward_;
							int temp_j = j_;
							byte charBeforeChange = content_file_as_bytes[j_+step_forward_]; // if we will not change
							
							// recursion:
							checkIfUpgrade(tmp_l, temp_step, temp_j, temp_index_of_upgrade_, 
									temp_save_old_char, temp_save_char_after_switch , save_old_char, save_index_of_upgrade, save_char_after_twist);
							// save values:
								putNumbersAtUpgrade(temp_index_of_upgrade_, temp_save_old_char, temp_save_char_after_switch, save_old_char,
										save_index_of_upgrade, save_char_after_twist, charBeforeChange, j_, step_forward_);
						}
					}						
				}
				
		} while ((content_file_as_bytes[j_ + step_forward_] == content_file_as_bytes[j_- tmp_d + step_forward_]));
		 
			/*
			 * finished upgrade
			 */
		
			upgrade = true;
			was_first_change=true;
			
			// upgrade is true - we change the letters: at index[temp_index_of_iterate] we get char_after_switch_of_iterate
	}
	
	private void AddTo_compressed_content_bytes_to_output_file(byte[] compressed_content_bytes_to_output_file, int d, int l,
														int c, int index) {
		/*in this function we combine the d and l into 1 byte,
		 *and add this byte and c byte to compressed_content_bytes_to_output_file. */

		String d_str = Integer.toBinaryString(d);
		while (d_str.length() < 8) { //padding with '0' if needed
			d_str = "0" + d_str;
		}
		String l_str = Integer.toBinaryString(l);
		while (l_str.length() < 8) {//padding with '0' if needed
			l_str = "0" + l_str;
		}
		//combining the d and l
		String d_l_str = d_str.substring(bits_of_look_a_head_buffer, 8) + l_str.substring(bits_of_max_sliding_window, 8);
		int d_l_int = Integer.parseUnsignedInt(d_l_str, 2);
		byte d_l_byte = (byte) d_l_int;
		
		byte c_byte = (byte) c;
		
		compressed_content_bytes_to_output_file[index] = d_l_byte;
		compressed_content_bytes_to_output_file[index + 1] = c_byte;
		
		//printing for user.
		System.out.println("-----------(" + d + ", " + l + ", " + (char) c + ")---------------");
	}

	
	public void Decompress(String input_file_path, String output_file_path) {
		File input_file = new File(input_file_path); // the input_file as file
		
		
		//array of all the compressed bytes from input.
		byte[] compressed_file_as_bytes = new byte[(int) input_file.length()];
		
		try {
			FileInputStream fileInputStream = new FileInputStream(input_file);
			fileInputStream.read(compressed_file_as_bytes); // reading all the
															// file into compressed_file_as_bytes.
			fileInputStream.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Reading The File.");
			e1.printStackTrace();
		}
		
		byte[] returned_original_bytes_to_output_file = new byte[(int) input_file.length() * 35];//array
		//that will hold all the decompressed bytes.
		
		int index_of_returned_original_bytes = 0; //initializing index.
		
		for (int j = 0; j < compressed_file_as_bytes.length; j=j+2) {
			
			d = (int) compressed_file_as_bytes[j]; //reading byte
			
			/*we shift the byte left 24 bits and right 24 bits,
			 *to get rid of '1' that make the integer negative.*/
			d = d << 24; 
			d = d >>> 24;
			
			d = d >>> bits_of_look_a_head_buffer;//shifting right number of bits of look a head buffer,
		//so what is left is just the window.
		
			l = (int) compressed_file_as_bytes[j]; //reading byte
			
			/*we shift the byte left 24 bits + bits of window and right 24 bits+ bits of window,
			 *to get rid of '1' that make the integer negative and to get rid of bits of window,
			 *so what is left is just look a head buffer.*/
			l = l << 24+bits_of_max_sliding_window; 
			l = l >>> 24+bits_of_max_sliding_window;
		
			/*if we don't copy from window, just write the c byte that is next.*/
			if (d == 0) {
				returned_original_bytes_to_output_file[index_of_returned_original_bytes] = compressed_file_as_bytes[j + 1];
				index_of_returned_original_bytes++;
				
			} else { /*copy the length specified from window, than write the c byte that is next. */
				while (l > 0) {
					returned_original_bytes_to_output_file[index_of_returned_original_bytes] = 
							returned_original_bytes_to_output_file[index_of_returned_original_bytes- d];
					index_of_returned_original_bytes++;
					l--;
				}
				returned_original_bytes_to_output_file[index_of_returned_original_bytes] = compressed_file_as_bytes[j + 1];
				index_of_returned_original_bytes++;
			}
			
		}
		//writing to file.
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(
					output_file_path);
			fileOutputStream.write(returned_original_bytes_to_output_file, 0,
					index_of_returned_original_bytes);
			fileOutputStream.close();

		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Writing The File.");
			e1.printStackTrace();
		}
		System.out.println("Decompress succes");
	}
	
	
	public void Decompress_Upgraded(String input_file_path, String output_file_path) {
		/*this method is similar to decompress just with reading of array that hold letters to replace with.*/

		File input_file = new File(input_file_path);
		
		byte[] size_of_fixing_info = new byte[4];//later will be an integer that tell us
		                                        //the size of fixing info.
		
		byte[] size_1_2_3_or_4=new byte[1];//byte that tell us if indices are the size of 1,2,3 or4.
		
		byte[] compressed_file_as_bytes=null;
		byte[] fixing_info=null;
		
		try {
			FileInputStream fileInputStream = new FileInputStream(input_file);
			fileInputStream.read(size_of_fixing_info);
			fileInputStream.read(size_1_2_3_or_4);
			ByteBuffer wrapped = ByteBuffer.wrap(size_of_fixing_info);
			int int_size_of_fixing_info = wrapped.getInt();
			
			fixing_info = new byte[int_size_of_fixing_info];
			fileInputStream.read(fixing_info);
			wrapped.clear();
			compressed_file_as_bytes = new byte[(int) input_file.length()-int_size_of_fixing_info-3];
			fileInputStream.read(compressed_file_as_bytes); // reading all the
															// file into compressed_file_as_bytes.
			fileInputStream.close();
			
		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Reading The File.");
			e1.printStackTrace();
		}
		
		
		byte[] returned_original_bytes_to_output_file = new byte[(int) input_file.length() * 35];//array
		//that will hold all the decompressed bytes.
		
		int index_of_returned_original_bytes = 0;//initializing index.
		
		for (int j = 0; j < compressed_file_as_bytes.length; j=j+2) {
			
			d = (int) compressed_file_as_bytes[j]; //reading byte
			
			/*we shift the byte left 24 bits and right 24 bits,
			 *to get rid of '1' that make the integer negative.*/
			d = d << 24; 
			d = d >>> 24;
			
			d = d >>> bits_of_look_a_head_buffer;//shifting right number of bits of look a head buffer,
		//so what is left is just the window.
		
			l = (int) compressed_file_as_bytes[j]; //reading byte
			
			/*we shift the byte left 24 bits + bits of window and right 24 bits+ bits of window,
			 *to get rid of '1' that make the integer negative and to get rid of bits of window,
			 *so what is left is just look a head buffer.*/
			l = l << 24+bits_of_max_sliding_window; 
			l = l >>> 24+bits_of_max_sliding_window;
		
			/*if we don't copy from window, just write the c byte that is next.*/
			if (d == 0) {
				returned_original_bytes_to_output_file[index_of_returned_original_bytes] = compressed_file_as_bytes[j + 1];
				index_of_returned_original_bytes++;
				
			} else { /*copy the length specified from window, than write the c byte that is next. */
				while (l > 0) {
					returned_original_bytes_to_output_file[index_of_returned_original_bytes] = 
							returned_original_bytes_to_output_file[index_of_returned_original_bytes- d];
					index_of_returned_original_bytes++;
					l--;
				}
				returned_original_bytes_to_output_file[index_of_returned_original_bytes] = compressed_file_as_bytes[j + 1];
				index_of_returned_original_bytes++;
			}
			
		}
		
		/*initializing variables.*/
		byte[] index_of_char_to_fix=new byte[4];
		int int_index_of_char_to_fix;
		byte original_byte;
		
		for (int i = 0; i <fixing_info.length-size_1_2_3_or_4[0]; i=i+size_1_2_3_or_4[0]+1) {//loop over array of
			                                                                              //fixing info.
			for (int j = 0; j < size_1_2_3_or_4[0]; j++) {//loop for retrieving the index depending on indices size,
				//that was sent from compressor
				
				index_of_char_to_fix[4-size_1_2_3_or_4[0]+j]=fixing_info[i+j];
			}
			
			ByteBuffer wrapped = ByteBuffer.wrap(index_of_char_to_fix);
			
			/*again, depending of size of indices, retrieving the letter from the right place in array.*/
			if(size_1_2_3_or_4[0]==1){
				 int_index_of_char_to_fix = wrapped.getInt();
			     original_byte=fixing_info[i+1];
			}
			else if(size_1_2_3_or_4[0]==2){
				 int_index_of_char_to_fix = wrapped.getInt();
				 original_byte=fixing_info[i+2];
			}
			else if(size_1_2_3_or_4[0]==3){
				 int_index_of_char_to_fix = wrapped.getInt();
				 original_byte=fixing_info[i+3];
			}
			else{
				 int_index_of_char_to_fix = wrapped.getInt();
				 original_byte=fixing_info[i+4];
			}
			
			returned_original_bytes_to_output_file[int_index_of_char_to_fix]=original_byte; //returning the right letter
			                                                                              //to it's original position.
			
			/*printing for user.*/
			System.out.println("just changed ["+int_index_of_char_to_fix +"],to the char\"" +(char) returned_original_bytes_to_output_file[int_index_of_char_to_fix]+"\"");
			
		}
		//writing to file.
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(
					output_file_path);
			fileOutputStream.write(returned_original_bytes_to_output_file, 0,
					index_of_returned_original_bytes);
			fileOutputStream.close();
			
			

		} catch (FileNotFoundException e) {
			System.out.println("File Not Found.");
			e.printStackTrace();
		} catch (IOException e1) {
			System.out.println("Error Writing The File.");
			e1.printStackTrace();
		}
	}


	private void putNumbersAtUpgrade(int[] temp_index_of_upgrade_, byte[] temp_save_old_char, byte[] temp_save_char_after_switch,
				byte[] save_old_char, int[] save_index_of_upgrade, byte[] save_char_after_twist, byte charBeforeChange,
					int j, int step_forward) {
		/*
		 * this method is called when we have new temporary upgrade and we need to save the new values at the arrays 
		 */
				temp_save_old_char[number_of_changes]=charBeforeChange;
				temp_index_of_upgrade_[number_of_changes]=j+step_forward;	
				temp_save_char_after_switch[number_of_changes]=content_file_as_bytes[j- tmp_d + step_forward];
				/*
			 			we change at index [number_of_changes] we put at 
			 			temp_save_char_after_switch: content_file_as_bytes[j- tmp_d + step_forward],
						at temp_index_of_upgrade_: (j+step_forward), and at temp-old-char:  charBeforeChange 
				 */
				number_of_changes++; // for the next change
	}
	
}