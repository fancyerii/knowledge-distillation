// package com.mobvoi.knowledgegraph.sc;
//
// import java.util.HashSet;
// import java.util.Set;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;
//
// import org.apache.commons.codec.digest.DigestUtils;
//
// import com.sun.org.apache.xml.internal.security.utils.Base64;
//
// public class Test {
//
// public static String enc(String s){
//// magic = bytearray('3go8&$8*3*3h0k(2)2')
//// song_id = bytearray(id)
//// magic_len = len(magic)
//// for i in xrange(len(song_id)):
//// song_id[i] = song_id[i] ^ magic[i % magic_len]
//// m = hashlib.md5(song_id)
//// result = m.digest().encode('base64')[:-1]
//// result = result.replace('/', '_')
//// result = result.replace('+', '-')
//// return result
//
// byte[] magic="3go8&$8*3*3h0k(2)2".getBytes();
// byte[] song_id=s.getBytes();
// int magic_len=magic.length;
// for(int i=0;i<song_id.length;i++){
// song_id[i] = (byte)(song_id[i] ^ magic[i % magic_len]);
// }
//
// String result=Base64.encode(DigestUtils.md5(song_id));
// result=result.replaceAll("\\/", "_");
// result=result.replaceAll("\\+", "-");
// return result;
//
//
// }
//
// public static void main(String[] args){
// Set<String> set=new HashSet<>();
// set.add("ab");
// System.out.println(set.contains(null));
//
// String s=enc("7920881767528662");
// System.out.println(s);
// String ss="abcd|||cde||";
// ss=ss.replaceAll("\\|\\|\\|", " ");
// System.out.println(ss);
// int days=1000;
// int hours=0;
// int minutes=0;
// int seconds=0;
// long interval=1000L*(seconds+60*minutes+3600*hours+24*3600*days);
// System.out.println(interval);
//
// Pattern variablePtn=Pattern.compile("\\$\\{\\d+\\}");
// Matcher m=variablePtn.matcher("a: ${12} c, ${2}");
// while(m.find()){
// System.out.println(m.group());
// }
// }
// }
