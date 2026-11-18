import 'package:flutter/material.dart';
import 'screens/home_screen.dart';
void main(){WidgetsFlutterBinding.ensureInitialized();runApp(const UmuravaApp());}
class UmuravaApp extends StatelessWidget{const UmuravaApp({super.key});@override Widget build(BuildContext context)=>MaterialApp(title:'UMURAVA',debugShowCheckedModeBanner:false,theme:ThemeData(colorScheme:ColorScheme.fromSeed(seedColor:const Color(0xFF176B4B)),useMaterial3:true),home:const HomeScreen());}
