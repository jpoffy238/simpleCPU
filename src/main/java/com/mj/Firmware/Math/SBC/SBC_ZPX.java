package com.mj.Firmware.Math.SBC;

import com.mj.Firmware.Framework.Instruction;
import com.mj.cpu001.CPU;
import com.mj.exceptions.DeviceUnavailable;
import com.mj.exceptions.illegalAddressException;
import com.mj.exceptions.nflagException;
import com.mj.exceptions.zflagException;

public class SBC_ZPX extends Instruction {
	public SBC_ZPX() {
		super((byte)0xf5);
		// TODO Auto-generated constructor stub
		setProperty(KEY_MNEMONIC, "SBC");
		setProperty(KEY_ADDRESSING_MODE, VALUE_ADDM_ZPX);
		setProperty(KEY_OPCODE, "0xf5");
		setProperty(KEY_INSTRUCTION_SIZE, "2");
		setProperty(KEY_CYCLES, "4");
		setProperty(KEY_WEB,"http://www.obelisk.me.uk/6502/reference.html#SBC" );
		setProperty(KEY_DESCRIPTION, "DC - Add with Carry\r A,Z,C,N = A+M+C");
	}

	public void exeute(CPU c) throws illegalAddressException, DeviceUnavailable {
		// TODO Auto-generated method stub
		// http://www.obelisk.me.uk/6502/reference.html#ADC
     	//
		// Addressing Mode 	Opcode  Bytes	Cycles
        // Zero Page	X              $75		2		    4
	
		/*ADC - Add with Carry
		 *  A,Z,C,N = A+M+C
		 * This instruction adds the contents of a memory 
		 * location to the accumulator together with the 
		 * carry bit. If overflow occurs the carry bit is set, 
		 * this enables multiple byte addition to be performed.
		 * 
		 * Processor Status after use:
		 * 
		 * C 	Carry Flag 	Set if overflow in bit 7
		 * Z 	Zero Flag 	Set if A = 0
		 * I 	    Interrupt Disable 	Not affected
		 * D 	Decimal Mode Flag 	Not affected
		 * B 	Break Command 	Not affected
		 * V 	Overflow Flag 	Set if sign bit is incorrect
		 * N 	Negative Flag 	Set if bit 7 set
		 * 
		 * [Return to Main Page] The Overflow (V) Flag Explained by Bruce Clark
[Up to Tutorials and Aids]
Table of Contents

1	  	Introduction
2	  	Instructions that affect V
2.1	  	CLV
2.2	  	PLP and RTI
2.3	  	BIT
2.4	  	ADC and SBC
2.4.1	  	Unsigned numbers, addition, and subtraction
2.4.2	  	Twos complement (signed numbers)
2.4.2.1	  	Twos complement numbers
2.4.2.2	  	Twos complement addition and subtraction
3	  	The hardware caveat: the SO pin
3.1	  	An SO pin example
4	  	V and the 65816
5	  	Why should I take your word for it?
Appendix A: Another way of thinking about twos complement
Appendix B: What about decimal mode?

1 INTRODUCTION

One of the more mysterious topics of the 6502 is the overflow (V) flag. It really isn't so difficult to understand, but it often seems more complicated than it actually is. Why? First, it's not needed very often, so it's not frequently used -- but it sure is handy when you do need it! Second, there are multiple (and distinct) purposes for the overflow flag in the 6502 (unlike the other flags). In fact, the BIT instruction -- the most common use of the overflow flag by 6502 software -- affects the overflow flag, but in a way that has nothing to do with arithmetic overflow. Third, the 6502 has a HARDWARE subtlety (which will be covered below) regarding the overflow flag that some programmers may not even be aware of. Fourth, a lot of 6502 reference material doesn't even describe the overflow flag! Fifth, the descriptions of the overflow flag that do exist are usually not very detailed and, most unfortunate of all, occasionally contain incorrect information. The material below is intended to fill these voids by providing a detailed description of the overflow flag, its purposes, and its uses.

Note: for the sake of brevity, the overflow flag will be referred to as V.

Former Commodore engineer Bil Herd shared this story about the SO pin that can be used to set the V flag:

One day in '83 or '84 I asked a question about the 6502 and Benny Pruden said "come with me". We went to the CAD area and ask Michael Angelina (head of CALMA/CAD) and his reply was, "yeah we got that here somewhere". I had no idea that he meant the hand drawn schematic of the 6502, until they opened the drawer! (I swear it was written on parchment) We looked at it for a bit when Benny started laughing and tapped the pin that had CPS in pencil over it, that later became the SO pin. CPS he explained, meant the Chuck Peddle Special pin.

Benny was the only guy that ever used it that I knew, he did the tightest loop possible in a piece of disk drive code.

2 INSTRUCTIONS THAT AFFECT V

Believe it or not, there are only six instructions that affect V: ADC, BIT, CLV, PLP, RTI, and SBC. The effect (on V) of each of these instructions is discussed below, starting with the simplest case.

2.1 CLV

CLV clears V. Simple enough. It is most frequently used in conjunction with a BVC to simulate a forced branch (a.k.a a branch always) in relocatable routines. The 65C02 has a branch always instruction (BRA), but the 6502 does not.

2.2 PLP and RTI

PLP and RTI pull the value of the processor status register from the stack, which affects all flags, including V. V is bit 6 of the processor status register, so after a PLP or an RTI, V will contain the value of bit 6 of the byte pulled from the stack.

2.3 BIT

On the 6502, BIT has two possible addressing modes: zero page or absolute. On the 65C02 (and 65816), BIT has three additional addressing modes: zero page indexed by X -- i.e. zp,X --, absolute indexed by X -- i.e. abs,X -- and immediate.

The immediate addressing mode (65C02 or 65816 only) does not affect V!

The remaining addressing modes affect V as follows: after a BIT instruction, V will contain bit 6 of the byte at the memory location specified in the operand.

For example, if memory location $1000 contains the (hexadecimal) value $AB, then a BIT $1000 instruction will clear V, since bit 6 of $AB is 0. On the other hand, if memory location $1000 contains $40, then BIT $1000 will set V, since bit 6 of $40 is 1.

A common use of BIT is to follow it with a BVC or BVS instruction to test bit 6 of a particular memory location. Another use of BIT is to set V, since the 6502 has a CLV instruction, but no corresponding SEV instruction. (Note that the other flag clearing instructions -- CLC, CLD, and CLI -- have corresponding flag setting instructions, namely SEC, SED, and SEI.) In this case, the V flag is typically set by a BIT of a ROM address that contains a byte (typically an RTS, i.e. $60) with bit 6 set.

2.4 ADC and SBC

The word "overflow" in overflow flag comes from addition and subtraction overflow. Ironically, it is the ADC and SBC instructions where the misinformation (and the confusion) about V often occurs. To understand V as it relates to ADC and SBC, a review of unsigned and twos complement (signed) numbers, as well as addition and subtraction, is in order.

2.4.1 UNSIGNED NUMBERS, ADDITION, AND SUBTRACTION

There are 256 possible values for a byte. In hexadecimal, these values are $00 to $FF (inclusive). These values could represent text, sound, or pictures, for example, but often they represent numbers. As it happens, there are several different ways in which numbers may be represented by the values $00 to $FF. The simplest way is that $00 to $FF represents the numbers 0 through 255. This is referred to as unsigned numbers, because the numbers do not have minus (or a plus) sign. Very conveniently, the 6502 can add and subtract unsigned numbers using the ADC and SBC instructions.

The ADC and SBC instructions not only return the result of the addition or subtraction (in the accumulator), but they also affect the carry flag. The carry flag has two purposes: first, it allows the 6502 to (easily) extend additions and subtractions beyond 8-bit numbers, and second, it indicates when the result is outside the range of 0 to 255.

The name "carry" comes from the first purpose, and it means the same thing that "carrying the one" did when you learned addition in school. For example, when adding:

    38
  + 27
you add the 8 and 7, which is 15, giving you the first digit, 5, and you carry the one:
    1
    38
  + 27
    --
     5
then you add the 1, 3, and 2 to get the next and final digit 6, giving you the result 65. In this way, the carry can be used to extend an addition or a subtraction beyond 8 bits. This allows you to work with, for example, 16-bit unsigned numbers, $0000 to $FFFF, which range from 0 to 65535.
As stated above, the second purpose of the carry flag is to indicate when the result of the addition or subtraction is outside the range 0 to 255, specifically:

When the addition result is 0 to 255, the carry is cleared.
When the addition result is greater than 255, the carry is set.
When the subtraction result is 0 to 255, the carry is set.
When the subtraction result is less than 0, the carry is cleared.
2.4.2 TWOS COMPLEMENT (SIGNED NUMBERS)

Even though unsigned numbers are sufficient for many applications, a question arises: what about negative numbers? As it happens, there are several ways of representing negative numbers, all of which will represent both negative and positive numbers, incidentally. These numbers are called signed numbers because they can have a minus (or a plus) sign. The specific method of representing signed numbers that will be discussed here is called twos complement.

2.4.2.1 TWOS COMPLEMENT NUMBERS

One way of defining -1 is to say that it is the number that when added to 1, gives 0 as a result. Confining ourselves to 8-bit results, we are looking for the number that, when added to $01, gives $00 as a result. That number is $FF, since $01 + $FF = $00. We are ignoring the carry for now, but we will return to it later. Likewise, $FE is -2 since $02 + $FE = $00. We can continue all the way to $7F + $81 = $00, where $7F is 127 and $81 is -127. This leaves $80. Is it -128 or 128? In twos complement, $80 represents -128. We'll get to the reason why this choice was made shortly. So the range of an 8-bit twos complement number is -128 to 127, where -128 to -1 are represented by $80 to $FF, and 0 to 127 are resprented by $00 to $7F. This may seem somewhat strange, but it has several useful properties:

First, the range 0 to 127 (the overlap of the unsigned and twos complement ranges) is represented by $00 to $7F, regardless of whether it is unsigned or twos complement.

Second, the most significant bit (bit 7 for an 8-bit number) indicates whether the number is negative (when bit 7 is one) or non-negative (when bit 7 is zero). Since bit 7 of $80 is set, $80 was chosen to be -128. Therefore, the sign of the number is conveniently stored in the most significant bit. Note that zero has the same "sign" (a zero in bit 7) as the positive numbers (1 to 127).

Third, because the numbers were chosen so that, e.g., $01 + $FF = $00, ADC and SBC can be used to add twos complement numbers just as easily as they can be used to add unsigned numbers.

Twos complement can also be extended beyond 8 bits. A 16-bit twos complement number ranges from -32768 to 32767. The range -32768 to -1 is represented by $8000 to $FFFF, and the range 0 to 32767 is represented by $0000 to $7FFF. Bit 15 is the most significant bit and indicates whether the number is negative or non-negative.

Note that it is necessary to be aware of the "width" of a twos complement number (i.e. whether it is an 8-bit number, a 16-bit number, a 24-bit number, etc.), since $80 represents the 8-bit twos complement number -128, but $0080 represents the 16-bit twos complement number 128. With unsigned numbers, such strictness isn't as necessary since $80 represents the 8-bit unsigned number 128, and $0080 represents the 16-bit unsigned number 128.

2.4.2.2 TWOS COMPLEMENT ADDITION AND SUBTRACTION

Remember the two purposes of the carry flag? The first purpose was to allow addition and subtraction to be extended beyond 8 bits. The carry is still used for this purpose when adding or subtracting twos complement numbers. For example:

  CLC       ; RESULT = NUM1 + NUM2
  LDA NUM1L
  ADC NUM2L
  STA RESULTL
  LDA NUM1H
  ADC NUM2H
  STA RESULTH
The carry from the ADC NUM2L is used by the ADC NUM2H.
The second purpose was to indicate when the number was outside the (unsigned) range, 0 to 255. But the range of a 8-bit twos complement number is -128 to 127, and the carry does not indicate whether the result is outside this range, as the following examples illustrate:

  CLC      ; 1 + 1 = 2, returns C = 0
  LDA #$01
  ADC #$01

  CLC      ; 1 + -1 = 0, returns C = 1
  LDA #$01
  ADC #$FF

  CLC      ; 127 + 1 = 128, returns C = 0
  LDA #$7F
  ADC #$01

  CLC      ; -128 + -1 = -129, returns C = 1
  LDA #$80
  ADC #$FF
This is where V comes in. V indicates whether the result of an addition or subraction is outside the range -128 to 127, i.e. whether there is a twos complement overflow. A few examples are in order:
  CLC      ; 1 + 1 = 2, returns V = 0
  LDA #$01
  ADC #$01

  CLC      ; 1 + -1 = 0, returns V = 0
  LDA #$01
  ADC #$FF

  CLC      ; 127 + 1 = 128, returns V = 1
  LDA #$7F
  ADC #$01

  CLC      ; -128 + -1 = -129, returns V = 1
  LDA #$80
  ADC #$FF

  SEC      ; 0 - 1 = -1, returns V = 0
  LDA #$00
  SBC #$01

  SEC      ; -128 - 1 = -129, returns V = 1
  LDA #$80
  SBC #$01

  SEC      ; 127 - -1 = 128, returns V = 1
  LDA #$7F
  SBC #$FF
Remember that ADC and SBC not only affect the carry flag, but they also use the value of the carry flag (i.e. the value before the ADC or SBC), and this will affect the result and will affect V. For example:
  SEC      ; Note: SEC, not CLC
  LDA #$3F ; 63 + 64 + 1 = 128, returns V = 1
  ADC #$40

  CLC      ; Note: CLC, not SEC
  LDA #$C0 ; -64 - 64 - 1 = -129, returns V = 1
  SBC #$40
The same principles apply when adding or subtracting 16-bit two complement numbers. For example:
  SEC         ; RESULT = NUM1 - NUM2
  LDA NUM1L   ; After the SBC NUM2H instruction:
  SBC NUM2L   ;   V = 0 if -32768 <= RESULT <= 32767
  STA RESULTL ;   V = 1 if RESULT < -32768 or RESULT > 32767
  LDA NUM1H
  SBC NUM2H
  STA RESULTH
3 THE HARDWARE CAVEAT: THE SO PIN

The previous section detailed the ways in which software can affect V; however, the 6502 and the 65C02 have a seldom-used pin that allows the hardware to affect V (independently of the software). This pin is called the SO pin, which stands for Set Overflow. Ironically, despite the popularity of the 6502, there isn't a consistent nomenclature for the SO pin in 6502 documentation, so you may see any of the following names for the SO pin (or combinations thereof):

  __
  SO   /SO   -SO   *SO   SOB   SOBAR   S.O.
In a (40-pin) DIP, the SO pin is pin 38. As its name implies, the SO pin sets V. It does this on a negative transition, i.e. when the voltage on the SO pin goes from high to low. So if V is being set when you don't expect it, one thing to check is the SO pin and whatever circuitry is connected to it. Note that the 65C02 has an internal pull-up on the SO pin, which means that the SO pin is unused (and therefore doesn't affect V) when nothing is connected to it. However, it is still recommended that the SO pin be pulled high (or pulled low) when unused.
Although the SO pin is something to be aware of, the software usually doesn't have to bother with it. First, as stated above, the SO pin is almost never used. Second, when it is used, it will be used to set V at a specific time, for a specific purpose, i.e. it won't be setting V at random times. In that case, it won't be interfering with V when the software doesn't expect it. The rule of thumb is: don't worry about the SO pin, because it's probably not used, but when in doubt, check the SO pin.

3.1 AN SO PIN EXAMPLE

For the more adventurous, here is one way the SO pin could be used. In 6502 circuits, I/O is typically memory-mapped. That is, RAM (needed for the stack and useful for the zero page) is usually at address $0000 and above, ROM (needed for the RESET, IRQ and NMI vectors) is at address $FFFF and below, and the I/O addresses are between the end of RAM and the start of ROM. So I/O is simply read from and written to as though it were memory.

There are applications where the software must wait until a hardware event has occured. One method for doing this is called polling, which means that the software keeps checking for the hardware event, continuing only after that event has occured. This typically looks something like this:

  LOOP BIT FLAG ; test the hardware flag
       BMI LOOP ; loop until the hardware flag goes low
When FLAG is an absolute address (it usually is), this loop takes 7 cycles, 4 for the BIT instruction, and 3 for the branch (assuming it doesn't cross a page boundary).
An alternative is to use the SO pin. In this case, V must be cleared by software, since the hardware has no way of doing it. The software will look something like this:

       CLV
  LOOP BVC LOOP ; loop until the hardware sets V
Notice that the address FLAG is no longer necessary, and could be used for some other purpose. Note also that the loop is 3 cycles (again, assuming a page boundary is not crossed), which means that the software can respond to the hardware event slightly faster than when checking FLAG. However, the hardware event must cause a negative transition on the SO pin. Using FLAG allows greater hardware flexibility. Certainly it is much easier to adapt the software (to the FLAG hardware) than to adapt the hardware (to the SO pin).
Of course this SO pin example is a bit farfetched, but use of the SO pin is rare.

4 V AND THE 65816

On the 65816, V behaves the same as on the 65C02 (and the 6502). Here are a few points to keep in mind when working with V on the 65816.

First, the 65816 has no SO pin, so there is no need to worry about any of that nonsense.

Second, the 65816 has two additional opcodes that can affect V: REP and SEP, which are typically used to clear or set the m and x flags, rather than V. If bit 6 of the REP operand is set, V will be clear, otherwise V will not be affected. If bit 6 of the SEP operand is set, V will be set, otherwise V will not be affected. Here are a few examples:

  REP #$40 ; clear V (2 bytes, 3 cycles, but otherwise the same as CLV)

  REP #$41 ; clear V and clear the carry flag

  SEP #$40 ; set V (without affecting anything else!)

  SEP #$41 ; set V and set the carry flag

  SEP #$01 ; set the carry flag (V is unaffected)
Third, when the m flag is 0, remember that the 65816 is operating on 16-bit data. This means that after, for example, BIT MEM, V contains bit 14 of MEM, instead of bit 6. So, oddly enough, when the m flag is 0, a BIT $F0 instruction has the same effect on V that a BIT $F1 instruction does when the m flag is 1, since bit 14 of memory location $F0 is bit 6 of memory location $F1. Also, note that when the m flag is 0, ADC and SBC add and subtract 16-bit numbers, so V indicates whether the result is outside the range -32768 ($8000) to 32767 ($7FFF).
5 WHY SHOULD I TAKE YOUR WORD FOR IT?

You shouldn't. You should try it for yourself. In that spirit, here is a program that proves that ADC and SBC affect the V flag as described, by checking every case. This program is also intended to illustrate how to break down a problem like this, and how to write a program to test the sort of claims made here. Hopefully, it can serve as a model of how to be skeptical. When someone makes claims like, "This is how the V flag works...", you don't have to accept it on blind faith!

Note that there are ways of doing this that are faster and shorter. However, the main objective here is for two things to be easy to understand: (a) how this program works, and (b) that it proves that ADC and SBC really do affect the V flag as described. To that end, efficiency has been forsaken in the interests of clarity.

The approach is relatively straightforward: test every case for 8-bit numbers. ADC and SBC are both tested, both with the carry clear (before the ADC or SBC instruction) and the carry set. Two 8-bit twos complement numbers, S1 and S2, are added and subtracted. All 256 possible values of S1 and all 256 possible values of S2 are tested. Two unsigned numbers, U1 and U2 are also used. S1 is initialized to -128 ($80) and is incremented until 127 ($7F) has been tested. While S1 is being incremented, U1, which is initialized to 0 ($00), is also being incremented, so U1 will be incremented up to, and including, 255 ($FF). The same is true for S2 and U2, so notice that U1 = S1 + 128 and U2 = S2 + 128.

The overflow flag from the twos complement addition is compared to the result of the unsigned addition. Remember that:

  V = 0 when S1 + S2 >= -128 and S1 + S2 <= 127
  V = 1 when S1 + S2 <  -128 or  S1 + S2 >  127
but since U1 = S1 + 128 and U2 = S2 + 128:
  U1 + U2 = (S1 + 128) + (S2 + 128) = S1 + S2 + 256
so:
  S1 + S2 = U1 + U2 - 256
therefore:
  V = 0 when U1 + U2 - 256 >= -128 and U1 + U2 - 256 <= 127
  V = 1 when U1 + U2 - 256 <  -128 or  U1 + U2 - 256 >  127
thus:
  V = 0 when U1 + U2 >= 128 and U1 + U2 <= 383 ($17F)
  V = 1 when U1 + U2 <  128 or  U1 + U2 >  383 ($17F)
The overflow flag from the twos complement subtraction is also compared to the result of the unsigned subtraction. Remember that:
  V = 0 when S1 - S2 >= -128 and S1 - S2 <= 127
  V = 1 when S1 - S2 <  -128 or  S1 - S2 >  127
To ensure that the unsigned result will be a positive number, the unsigned subtraction that is performed is:
  (65280 + U1) - U2
(Note that 65280 = $FF00.) Since U1 = S1 + 128 and U2 = S2 + 128:
  U1 - U2 = S1 + 128 - (S2 + 128) = S1 + 128 - S2 - 128 = S1 - S2
so:
  V = 0 when U1 - U2 >= -128 and U1 - U2 <= 127
  V = 1 when U1 - U2 <  -128 or  U1 - U2 >  127
thus:
  V = 0 when (65280 + U1) - U2 >= 65152 and (65280 + U1) - U2 <= 65407
  V = 1 when (65280 + U1) - U2 <  65152 or  (65280 + U1) - U2 >  65407
(Note: 65152 = $FE80 and 65407 = $FF7F)
The program below takes about 16 seconds to complete at 1 MHz.

; Demonstrate that the V flag works as described
;
; Returns with ERROR = 0 if the test passes, ERROR = 1 if the test fails
;
; Five (additional) memory locations are used: ERROR, S1, S2, U1, and U2
; which can be located anywhere convenient in RAM
;
TEST CLD       ; Clear decimal mode (just in case) for test
     LDA #1
     STA ERROR ; Store 1 in ERROR until test passes
     LDA #$80
     STA S1    ; Initalize S1 and S2 to -128 ($80)
     STA S2
     LDA #0
     STA U1    ; Initialize U1 and U2 to 0
     STA U2
     LDY #1    ; Initialize Y (used to set and clear the carry flag) to 1
LOOP JSR ADD   ; Test ADC
     CPX #1
     BEQ DONE  ; End if V and unsigned result do not agree (X = 1)
     JSR SUB   ; Test SBC
     CPX #1
     BEQ DONE  ; End if V and unsigned result do not agree (X = 1)
     INC S1
     INC U1
     BNE LOOP  ; Loop until all 256 possibilities of S1 and U1 are tested
     INC S2
     INC U2
     BNE LOOP  ; Loop until all 256 possibilities of S2 and U2 are tested
     DEY
     BPL LOOP  ; Loop until both possiblities of the carry flag are tested
     LDA #0
     STA ERROR ; All tests pass, so store 0 in ERROR
DONE RTS
;
; Test ADC
;
; X is initialized to 0
; X is incremented when V = 1
; X is incremented when the unsigned result predicts an overflow
; Therefore, if the V flag and the unsigned result agree, X will be
; incremented zero or two times (returning X = 0 or X = 2), and if they do
; not agree X will be incremented once (returning X = 1)
;
ADD  CPY #1   ; Set carry when Y = 1, clear carry when Y = 0
     LDA S1   ; Test twos complement addition
     ADC S2
     LDX #0   ; Initialize X to 0
     BVC ADD1
     INX      ; Increment X if V = 1
ADD1 CPY #1   ; Set carry when Y = 1, clear carry when Y = 0
     LDA U1   ; Test unsigned addition
     ADC U2
     BCS ADD3 ; Carry is set if U1 + U2 >= 256
     BMI ADD2 ; U1 + U2 < 256, A >= 128 if U1 + U2 >= 128
     INX      ; Increment X if U1 + U2 < 128
ADD2 RTS
ADD3 BPL ADD4 ; U1 + U2 >= 256, A <= 127 if U1 + U2 <= 383 ($17F)
     INX      ; Increment X if U1 + U2 > 383
ADD4 RTS
;
; Test SBC
;
; X is initialized to 0
; X is incremented when V = 1
; X is incremented when the unsigned result predicts an overflow
; Therefore, if the V flag and the unsigned result agree, X will be
; incremented zero or two times (returning X = 0 or X = 2), and if they do
; not agree X will be incremented once (returning X = 1)
;
SUB  CPY #1   ; Set carry when Y = 1, clear carry when Y = 0
     LDA S1   ; Test twos complement subtraction
     SBC S2
     LDX #0   ; Initialize X to 0
     BVC SUB1
     INX      ; Increment X if V = 1
SUB1 CPY #1   ; Set carry when Y = 1, clear carry when Y = 0
     LDA U1   ; Test unsigned subtraction
     SBC U2
     PHA      ; Save the low byte of result on the stack
     LDA #$FF
     SBC #$00 ; result = (65280 + U1) - U2, 65280 = $FF00
     CMP #$FE
     BNE SUB4 ; Branch if result >= 65280 ($FF00) or result < 65024 ($FE00)
     PLA      ; Get the low byte of result
     BMI SUB3 ; result < 65280 ($FF00), A >= 128 if result >= 65152 ($FE80)
SUB2 INX      ; Increment X if result < 65152 ($FE80)
SUB3 RTS
SUB4 PLA      ; Get the low byte of result (does not affect the carry flag)
     BCC SUB2 ; The carry flag is clear if result < 65024 ($FE00)
     BPL SUB5 ; result >= 65280 ($FF00), A <= 127 if result <= 65407 ($FF7F)
     INX      ; Increment X if result > 65407 ($FF7F)
SUB5 RTS
APPENDIX A: ANOTHER WAY OF THINKING ABOUT TWOS COMPLEMENT

For the curious, here's a completely different approach to twos complement.

Remember when you learned about binary numbers? It'd usually start with an example of a decimal number, like 203, then point out that:

3 is the ones digit
0 is the tens digit
2 is the hundreds digit
That is, 203 = 2 * 100 + 0 * 10 + 3 * 1
Then there'd be an example of a binary number, like 10101011, and another digit-by-digit breakdown:

1 is the ones digit
1 is the twos digit
0 is the fours digit
1 is the eights digit
0 is the sixteens digit
1 is the thirty-twos digit
0 is the sixty-fours digit
1 is the one-hundred-twenty-eights digit
And, 171 = 1 * 128 + 0 * 64 + 1 * 32 + 0 * 16 + 1 * 8 + 0 * 4 + 1 * 2 + 1 * 1
Sure enough, 171 is the unsigned number represented by $AB (10101011). The twos complement number represented by $AB is -85. As it happens, a twos complement number can be broken down in a simliar way:

1 is the ones digit
1 is the twos digit
0 is the fours digit
1 is the eights digit
0 is the sixteens digit
1 is the thirty-twos digit
0 is the sixty-fours digit
1 is the MINUS-one-hundred-twenty-eights digit
That is:
-85 = 1 * -128 + 0 * 64 + 1 * 32 + 0 * 16 + 1 * 8 + 0 * 4 + 1 * 2 + 1 * 1
A 16-bit two complement number is similar, but in that case, the sixteenth binary digit (i.e. bit 15) is the -32768s digit (and the eighth binary digit is the 128s digit, not the -128s digit, of course).
APPENDIX B: WHAT ABOUT DECIMAL MODE?

A curious statement appears in the 65C02 datasheets. When describing the differences between the 6502 and 65C02, they state that when the D flag is 1 (decimal mode), the ADC and SBC take 1 additional cycle, but the N, V, and Z flags will be valid. (The C flag was the only valid flag on the 6502.)

BCD is essentially an unsigned representation; that is, $99 represents 99, not -1. Since the carry flag is the relevant flag for unsigned addition and subtraction, and the concept of arithmetic overflow (as it applies to V) really applies only to signed numbers, there is the question of how V is affected.

Interestingly, the effect on V in decimal mode is the same on a 6502, 65C02, and 65816, even when one or both of the numbers being added or subtracted are not valid BCD numbers! This has been tested on a Rockwell 6502, a Synertek 6502, a GTE 65C02, and a GTE 65C816.

There isn't likely to be much use for V with BCD addition or subtraction, but here is how ADC and SBC affect the V flag in decimal mode, starting with SBC, which is the simpler case. It should be noted that this is getting into the realm of undocumented behavior, so it is dangerous to rely on this behavior.

After:

  SED
  SBC NUM
the V flag is the same as after:
  CLD
  SBC NUM
assuming, of course, that the carry flag (before the SBC), the accumulator, and NUM are the same in both cases. The result (accumulator) will be different, so for the purpose of determining what V will be, perform the subtraction as though the numbers being subtracted were twos complement numbers, rather than BCD numbers. Remember that this is true even when the numbers are not valid BCD numbers.
ADC is a little more complicated. Its effect on V doesn't make much sense, but that effect can be determined by working one digit at time.

First, add the lower (least significant) digits. If the result is greater than 9, then carry a one. For example:

    24
  + 56
4 + 6 = 10, which is greater than 9, so carry the one:
    1
    24
  + 56
    --
     0
Then add the upper digits (carrying the one, when dictated by the lower digit result), treating the upper digits as 4-bit twos complement numbers (which can range from -8 to 7). V indicates whether there is an overflow from adding the upper digits (i.e. whether the upper digit result is outside the range -8 to 7). Continuing the example, 1 + 2 + 5 = 8, which is outside the range -8 to 7, so V = 1.
A second example:

    93
  + 82
3 + 2 = 5. Note that a one is not carried.
    93
  + 82
    --
     5
Remember that 9 and 8 must be treated as 4-bit twos complement numbers, so 9 + 8 is really -7 + -8 = -15, so V = 1.
A third example:

    89
  + 76
9 + 6 = 15, so carry the one:
    1
    89
  + 76
    --
     5
8 (and 7) must be treated as twos complement numbers, so 1 + 8 + 7 is really 1 + -8 + 7 = 0, so V = 0.
Remember that this is true even when the numbers being added are not valid BCD numbers. For example:

    80
  + F0
0 + 0 = 0. Note that a one is NOT carried.
    80
  + F0
    --
     0
8 and F must be treated as twos complement numbers, so 8 + F is really -8 + -1 = -9, so V = 1.
Another example:

    80
  + FA
0 + A is really 0 + 10 = 10, so carry the one:
    1
    80
    FA
    --
     0
8 and F must be treated as twos complement numbers, so 1 + 8 + F is really 1 + -8 + -1 = -8, so V = 0.
Also, note that when at least one of lower digits is invalid, it is possible that adding the lower digits will produce a result greater than 19. However, in this case, you still carry a one, NOT a two or a three. For example:

    2F
  + 4F
F + F is really 15 + 15, which is 30, but carry a one, NOT a three:
    1
    2F
  + 4F
    --
1 + 2 + 4 = 7, so V = 0. (As an aside, the lower digit of the result -- i.e. the accumulator -- will be 4, rather than 0. When the lower digit result is greater than 9, the 4 least significant bits of the accumulator will be the 4 least significant bits of the sum of 6 plus the lower digit result, i.e. $F + $F + 6 = $24)
Finally, don't forget to account for value of the carry flag before the ADC when doing the addition to determine the effect on V.

Last Updated April 4, 2004.
V = 0 when S1 + S2 >= -128 and S1 + S2 <= 127
  V = 1 when S1 + S2 <  -128 or  S1 + S2 >  127
but since U1 = S1 + 128 and U2 = S2 + 128:
  U1 + U2 = (S1 + 128) + (S2 + 128) = S1 + S2 + 256
so:
  S1 + S2 = U1 + U2 - 256
therefore:
  V = 0 when U1 + U2 - 256 >= -128 and U1 + U2 - 256 <= 127
  V = 1 when U1 + U2 - 256 <  -128 or  U1 + U2 - 256 >  127
thus:
  V = 0 when U1 + U2 >= 128 and U1 + U2 <= 383 ($17F)
  V = 1 when U1 + U2 <  128 or  U1 + U2 >  383 ($17F)
		 */
		int address = getZeroPageXAddress(c);
		int m = (byte)(c.bus.read(address) & 0xff);
		
		int a = c.a.get();

		
		
		int carryValue  = 0;
		if (c.CFLAG.isSet() ) {
			carryValue = 1;
		}
	//	V = 0 when U1 + U2 >= 128 and U1 + U2 <= 383 ($17F)
	//  V = 1 when U1 + U2 <  128 or  U1 + U2 >  383 ($17F)
		int result = a - m - (1 - carryValue);
	//   V = 1 when S1 + S2 <  -128 or  S1 + S2 >  127
		// V = 1 when U1 + U2 <  128 or  U1 + U2 >  383 ($17F)
		if ((result < 128 ) || (result > 383)) {
			c.OFLAG.clear();
		}
		if ((result <= 383 )&& (result >= 128)) {
			c.OFLAG.set();
		}
	
		// When the addition result is greater than 255, the carry is set.
		if (result > 255 ) {
			c.CFLAG.set();
		} else {
			c.CFLAG.clear();
	}
		
	
		try {
			c.a.set(result & 0xff);
		} catch (zflagException e) {
			// TODO Auto-generated catch block
			c.ZFLAG.set();
			c.NFLAG.clear();
		} catch (nflagException e) {
			// TODO Auto-generated catch block
			c.NFLAG.set();
			c.ZFLAG.clear();
		
		}
		c.pc++;
	}
}
