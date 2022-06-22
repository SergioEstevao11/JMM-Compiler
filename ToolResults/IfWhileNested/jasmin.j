.class public IfWhileNested
.super java/lang/Object

.method public <init>()V
	.limit stack 1
	.limit locals 1

	aload_0
	invokespecial java/lang/Object/<init>()V
	return
.end method

.method public func(I)I
	.limit stack 2
	.limit locals 5

	iconst_1
	istore_2
	iconst_0
	istore 4
Loop1:
	iload 4
	iload_1
	isub
	ifge EndLoop1
	iload_2
	iconst_0
	isub
	ifeq Else2
	iconst_1
	invokestatic ioPlus.printResult(I)V
	goto EndIf2
Else2:
	iconst_2
	invokestatic ioPlus.printResult(I)V
EndIf2:
	iload_2
	ifne NOTB1
	iconst_1
	goto Continue1
NOTB1:
	iconst_0
Continue1:
	istore_2
	iinc 4 1
	goto Loop1
EndLoop1:
	iconst_1
	ireturn
.end method

.method public static main([Ljava/lang/String;)V
	.limit stack 3
	.limit locals 4

	new IfWhileNested
	dup
	astore_1
	aload_1
	invokespecial IfWhileNested/<init>()V
	aload_1
	iconst_3
	invokevirtual IfWhileNested.func(I)I
	istore_2
	iload_2
	istore_3
	return
.end method
