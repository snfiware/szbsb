#!/usr/bin/python3
# -*- coding: utf-8 -*-
# /**
#  * Copyright 2020 (Corona-Version) Schnuffiware - snuffo@freenet.de
#  * <p/>
#  * Licensed under the Apache License, Version 2.0 (the "License");
#  * you may not use this file except in compliance with the License.
#  * You may obtain a copy of the License at
#  * <p/>
#  * http://www.apache.org/licenses/LICENSE-2.0
#  * <p/>
#  * Unless required by applicable law or agreed to in writing, software
#  * distributed under the License is distributed on an "AS IS" BASIS,
#  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  * See the License for the specific language governing permissions and
#  * limitations under the License.
#  */
from datetime import datetime, date
import inspect
import os
from os import environ
import sys
import re
print("sz.py - first contact - environ:%s"%(environ))
#from pathlib import Path # sudo apt-get install python-pathlib
#import urllib            # sudo apt-get install python-urllib3
#import urllib.request	  # sudo apt-get install python-urllib3
#
# >>>>>>>>>>>>>>>>
# WORKAROUND >>>>> bypass qpython android problem "no ciphers available" by writing own adpater
import requests
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.poolmanager import PoolManager
import ssl
class MyAdapter(HTTPAdapter):
	def init_poolmanager(self, connections, maxsize, block=False):
		self.poolmanager = PoolManager(num_pools=connections,
										maxsize=maxsize,
										block=block,
										ssl_version=ssl.PROTOCOL_TLSv1)
# Plus below:
# s = requests.Session()
# s.mount('https://', MyAdapter())
# WORKAROUND <<<<< bypass qpython android problem "no ciphers available"
# <<<<<<<<<<<<<<<<
#
##### FIRST HELPER FX #####
def out(s):
	print("[" + str(datetime.now()) + " fx:" + inspect.currentframe().f_back.f_code.co_name + "] " + s)
	#print("[" + str(datetime.now()) + " fx:" + __name__ + "] " + str(s))
#
##### Callback to Java / Kotlin #####
out("-> chaquo-class-stub-definition")
## CALL this within executeScript to show snackbar-msg:
# adh.showSnackMsgFromPythonViaPublishProgress("py::executeScript reached")
## import de.snfiware.szbsb.main.AsyncDownloadHandler
# from de.snfiware.szbsb.main import AsyncDownloadHandler
out("<- chaquo-class-stub-definition")
#
def is_directory_writable(dir,bCreateIfNotExists):
	bRc = False
	bCreated = False
	try:
		if bCreateIfNotExists:
			if not os.path.exists(dir):
				out("try making dir '%s'"%dir)
				os.makedirs(dir)
				bCreated = os.path.exists(dir)
		bRc = os.access(dir, os.W_OK)
	except Exception as e:
		out("EXC: "+str(e))
	#
	out("new: %s; write-perm: %s; dir '%s'"%(bCreated,bRc,dir))
	return bRc
#
def get_script_dir(follow_symlinks=True):
	if getattr(sys, 'frozen', False): # py2exe, PyInstaller, cx_Freeze
		path = os.path.abspath(sys.executable)
	else:
		path = inspect.getabsfile(get_script_dir)
	if follow_symlinks:
		path = os.path.realpath(path)
	return os.path.dirname(path)
#
def is_android():
	return( 'ANDROID_STORAGE' in environ or 'ANDROID_ROOT' in environ)
#
## On Android adh is an object of kotlin class AsyncDownloadHandler
## If this script is executed from linux shell no such object is available nor is it necessary
def showAndroidSnack(adh,stringText):
	out("(async.) REQUESTING TO SHOW SNACK: '%s'"%(stringText))
	if( is_android() ):
		adh.showSnackMsgFromPythonViaPublishProgress(stringText)
	#else noop = 0
#
def get_app_dir():
	if is_android():
		return( environ['HOME'] )
	else:
		return( get_script_dir() )
#
##### CONFIGURATION #####
def load_config():
	out( "Lese Konfiguration..." )
	configFileName = get_app_dir() + "/szconfig"
	# Configuration tokens to extract
	cfgUsername = "username"
	cfgPassword = "password"
	cfgDownloadFolder = "downloadFolder"
	cfgPages = "myPages"
	cfgTopics = "myTopics"
	# Definition & Defaults
	username = "user-is-not-set"
	password = "password-is-not-set"
	downloadFolderFromCfg = ""
	downloadFolder = "./sz"
	if is_android():
		downloadFolder = "/storage/emulated/0/Download/szDefaultFromSzPyScript"
	myPages = ['1','2','3']
	myTopics = ['Politik']
	# Load config parameters from file
	try:
		with open(configFileName, 'r') as configFile:
			content = configFile.read()
			out(content)
			lines = content.split("\n") #split it into lines
			for line in lines:
				line = line.strip() # trim
				parts = line.split(" = ")
				if len(parts) == 2:
					if cfgUsername == parts[0]:
						username = str(parts[1]).strip("'")
					elif cfgPassword == parts[0]:
						password = str(parts[1]).strip("'")
					elif cfgDownloadFolder == parts[0]:
						downloadFolderFromCfg = str(parts[1]).strip("'")
					elif cfgPages == parts[0]:
						pages = []
						for page in parts[1].split(","):
							pages.append( str(page).strip("'") )
						myPages = pages
					elif cfgTopics == parts[0]:
						topics = []
						for topic in parts[1].split(","):
							topics.append( str(topic).strip("'") )
						myTopics = topics
	except Exception as e:
		import traceback, sys
		out( "Error with config file '%s'; internal:\n%s\n%s"
			%(configFileName,sys.exc_info(),traceback.format_exc()) )
		out( "IGNORING - continuing with internal defaults..." )
	#
	out( "Relative Orte behandeln und Datum-Unterordner einfügen" )
	if len(downloadFolderFromCfg) > 1: # "/x" is the minimum of meaningfullness
		downloadFolder = downloadFolderFromCfg
	else:
		out("STILL USING DEFAULTS for download - cfg: %s"%downloadFolderFromCfg)
		#
	if downloadFolder[0:2] == ".." or downloadFolder[0] == ".": # relative from script location
		downloadFolder = get_app_dir() + "/" + downloadFolder
	downloadFolder = downloadFolder + "/" + str(date.today()) + "/"
	out( "username: '%s'; password: '%s'; myPages: %s; myTopics: %s; downloadFolder is '%s'"%
		  (username,password,myPages,myTopics,downloadFolder) )
	return(username,password,myPages,myTopics,downloadFolder)
#
def check_config(username,password,myPages,myTopics,downloadFolder):
	assert( len(username) > 0 ), "empty username"
	assert( len(username) == 12 ), "username must be 12-digits long '%s'"%username
	assert( is_numeric(username) ), "username must be numeric '%s'"%username
	assert( len(password) > 0 ), "empty or invalid password '%s'"%password
	assert( len(downloadFolder) > 0), "empty or invalid downloadFolder '%s'"%downloadFolder
	assert( is_directory_writable(downloadFolder,True) ), "NO write permissions to downloadFolder '%s'"%downloadFolder
	assert( len(myPages) > 0 or len(myTopics) > 0 ), "at least one of both: page or topics must not be empty"
	assert( len(myPages) != 1 or myPages[0] != 'GARNIX' or len(myTopics) != 1 or myTopics[0] != 'GARNIX' ), ""+\
		"at least one of both must be filled meaningfully: pages: '%s' or topics: '%s'"%(str(myPages),str(myTopics))
#
##### FURTHER HELPER FX #####
def urlStringReplace(myUrl):
	myUrl = myUrl.replace('%5F','_').replace('&amp;','&')
	return(myUrl)
#
##### GLOBAL DOWNLOAD DICTONARY + HELPERS #####
# wird benutzt um nix doppelt zu holen und die Reihenfolge zu verwalten
dictDownloads = {} 
# dictDownloads = {<page>:{'url':<url>,'from':<topic>|'page','idx':<number>}
FAILOVER = 90 # page number to start with on failover when trying to extract from html
#
def getKey(intOrStr):
	key = int(str(intOrStr))
	if key < 0 or key > 100: 
		out("invalid key boundaries: " + key) 
		quit()
	return("S"+("%02i"%(key)))
#
def getFileNameForPdf( k, i ):
	sRc = getKey(k)
	#
	if is_android():
		if dictDownloads[k]['from'] != "page":
			sRc += "-"+dictDownloads[k]['from']
	else:
		sRc += ("-("+dictDownloads[k]['from'])+("-"+str(dictDownloads[k]['idx']))+(")-I%02i"%i)
	#
	return str(sRc+".pdf").replace('/','+')
#
def memorizeUrl(anykey, n2ndKey, urlStartIdx, urlString, idx, topic='page', maxExtra=9):
	skey = getKey(anykey) # just for validating
	skey = getKey(n2ndKey)# just for validating
	key = int(str(anykey))
	key2= int(str(n2ndKey))
	assert( key <= key2 ), "key '%i' darf nicht größer als key2 '%i' sein."%(key,key2)
	if key in dictDownloads:
		if urlString == dictDownloads[key]['url']:
			if 'page' == topic or 'page' != dictDownloads[key]['from']:
				out("key '%i' already set to same url - returning without action"%(key))
				return
			else:
				out("upclassing page to topic")
		elif key2 not in dictDownloads:
			out("using secondary key '%i' instead of taken primary '%i'"%(key2,key))
			key = key2
		elif maxExtra > 0 and key2 < 99:
			out("key'%i'+key2'%i' already set, try another slot (extra avail: %i) - recalling memorize"%(key,key2,maxExtra))
			memorizeUrl(key2+1, key2+1, urlStartIdx, urlString, idx, topic, maxExtra-1)
			return
		elif key != key2 and key2 in dictDownloads:
			out("FATAL ERROR: both keys are taken '%i' & '%i' - LOSING CONTENT!"%(key,key2))
			return
		else:
			out("ALREADY EXISTING KEY %i - OVERWRITING NOW!\nold value: %s\nnew value: %s"
				%(key, dictDownloads[key]['url'], urlString)) 
	out("setting key '%i' to topic: %s(%i) and url[%i]: %s"%(key,topic,idx,urlStartIdx,urlString))
	dictDownloads[key] = {'url':urlString,'from':topic,'idx':idx,'pos':urlStartIdx}
#
def is_numeric( c ):
	bRc = False
	try:
		n = int(c)
		bRc = True
	except Exception as e:
		out("ign.: "+str(c)+"; exc.: "+str(e)) # ignore everything
	return bRc
# Sucht innerhalb des Webseiten-Texts ws die Seitenzahl seite und sucht nach intern
# definierten Regeln die passende href raus, setzt evtl. störende Zeichen um,
# setzt die URL Teile zusammen und gibt eine aufrufbare URL zurück.
# Fehler können vom Aufrufer mittels if rc == baseUrl: geprüft werden.
#
# Beispiel-XML-Fragment siehe unten 
#
def getUrlByPageNumber( ws, seite, baseUrl ):
	rcUrlStartIdx = -1
	rcUrlFragment = ""
	lookupTopic =   ['">' +str(seite)+"</a>" \
					,'"> '+str(seite)+"</a>" \
					,'">' +str(seite)+" </a>" \
					,str(seite)+"</a>" \
					,str(seite)+" </a>" ] #mit oder ohne Blank (vorne+hinten; Schmierzeichen ign.)
	lookupARef	= '<a href="' # oben haben wir eh schon das Ende der URL gefunden
	for t in lookupTopic:
		idxFound = ws.find(t)
		if idxFound != -1:
			out("page-idxFound: "+str(idxFound)+"; len(ws): "+str(len(ws)) + "; hitting-expr: " + t)
			if is_numeric(ws[idxFound-1:idxFound]):
				out("preceeding number - discarding this match...")
			else:
				idxARef = ws.rfind(lookupARef,idxFound-500,idxFound-1)
				if idxARef != -1:
					out("a-href found at idx: "+str(idxARef))
					idxEndUrl = ws.find('"',idxARef+len(lookupARef)+1)
					if idxEndUrl != -1:
						rcUrlStartIdx = idxARef+len(lookupARef)
						rcUrlFragment = ws[rcUrlStartIdx : idxEndUrl]
						out("EXTRACTED URL FRAGMENT (" + str(idxARef) + "," + str(idxEndUrl) + "): " + rcUrlFragment)
						break # first best is enough - list lookupTopic is ordered appropriately
					else:
						out("cannot find end of url")
						break
				else:
					out("cannot find <a...")
					break
		else: 
			out("cannot find pageNumber with expr: " + t)
	#
	return( rcUrlStartIdx, baseUrl + urlStringReplace(rcUrlFragment) )
#
# Beispiel-Fragment: >Politik< ---> URL-HREF->URL-Ende ---> Seite ">@ 1</a"
#                    >Themen des Tages< (BACK)<--- URL-HREF->URL-Ende ---> Seite ">2 </a"
#
# testWebSiteText = '<td class="S6387580" onmouseover="markTD(\'S6387580\')" onmouseout="unmarkTD(\'S6387580\')">\
# 	<span style="display:block; text-align:right; "><span style="display:inline-block; min-height:16px; min-width:70px; vertical-align:bottom; font-size:65%; ">Politik</span> \
# 	<a href="hh03.ashx?req=pagehtm&amp;bid=SZ20191228S6387580.SZ.SZ.def.def.0..NT&amp;uid=libnetbsbmuenchen&amp;usi=10021&amp;ugr=ugroup%5Fabo%5Flibnetretro&amp;z=Z22492">Â 1</a></span>									<a href="hh03.ashx?req=pagehtm&amp;bid=SZ20191228S6387580.SZ.SZ.def.def.0..NT&amp;uid=libnetbsbmuenchen&amp;usi=10021&amp;ugr=ugroup%5Fabo%5Flibnetretro&amp;z=Z10408" style="background:URL(hh03.ashx?req=pagepdf&amp;bid=SZ20191228S6387580&amp;res=T); background-position:0px 0px; border:1px solid lightgray; display:block; height:160px; width:110px; "> </a></td>								<td style="min-width:10px; "> </td>								<td class="S6387581" onmouseover="markTD(\'S6387581\')" onmouseout="unmarkTD(\'S6387581\')">									<span>\
# 	<a href="hh03.ashx?req=pagehtm&amp;bid=SZ20191228S6387581.SZ.SZ.def.def.0..NT&amp;uid=libnetbsbmuenchen&amp;usi=10021&amp;ugr=ugroup%5Fabo%5Flibnetretro&amp;z=Z62807">2 </a><span style="display:inline-block; min-height:16px; min-width:70px; vertical-align:bottom; font-size:65%; ">Themen des Tages</span></span>										<br /> \
# 	<a href="hh03.ashx?req=pagehtm&amp;bid=SZ20191228S6387581.SZ.SZ.def.def.0..NT&amp;uid=libnetbsbmuenchen&amp;usi=10021&amp;ugr=ugroup%5Fabo%5Flibnetretro&amp;z=Z00822"  	 style="background:URL(hh03.ashx?req=pagepdf&amp;bid=SZ20191228S6387581&amp;res=T); background-position:0px 0px; border:1px solid lightgray; display:block; height:160px; width:110px; "> </a></td>'
#
# Sucht innerhalb des Webseiten-Texts ws das Thema topic und sucht nach intern
# definierten Regeln die passenden hrefs raus, setzt evtl. störende Zeichen um,
# setzt die URL Teile zusammen und gibt eine Liste von aufrufbaren URLs zurück.
# Keine Fehlerrückgabe. Intern wird über memorizeUrl die 
# globale Download-Collection befüllt.
#
def getUrlByTopic( ws, topic, baseUrl, callerIndex, callerTotal ): 
	lookupTopic = ">"+topic+"</span>"
	lookupARef	= '<a href="hh03.ashx?req=pagehtm'
	i = 0
	idxFound = 1
	while idxFound != -1:
		i += 1
		out("LOOP Durchlauf: %i für %s"%(i,topic))
		idxFoundPrev = idxFound
		idxFound = ws.find(lookupTopic,idxFound)
		if idxFound != -1:
			out("%i.hit; idxFound: %i"%(i,idxFound))
			idxARef = -2;
			# abhängig vom Kontext nach vorne oder nach hinten suchen
			afterFound = idxFound+len(lookupTopic)
			if ws[afterFound : afterFound+7] == "</span>": # vgl. beispiel oben </span></span>
				idxARef = ws.rfind(lookupARef,idxFound-500,idxFound) 
				out("zweites span gefunden, rückwarts suchen - idxARef %i"%idxARef)
			else: # vorwärts
				idxARef = ws.find(lookupARef,idxFound+len(lookupTopic)) 
				out("nur ein span, vorwärts suchen - idxARef %i"%idxARef)
			idxFound += 1 # watchout: below no usage of idxFound: infinite loop!
			if idxARef != -1:
				idxEndUrl = ws.find('"',idxARef+len(lookupARef))
				if idxEndUrl != -1:
					urlStartIdx = idxARef+9
					urlFragment = ws[urlStartIdx : idxEndUrl]
					out("EXTRACTED URL FRAGMENT (" + str(urlStartIdx) + "," + str(idxEndUrl) + "): " + urlFragment)
					#
					out("find corresponding page...")
					foundPageNumber = FAILOVER
					foundPageNumber2 = FAILOVER
					buf = ws[idxEndUrl+2 : idxEndUrl+50]
					#out("buf: "+buf)
					idxNextLt = buf.find('<')
					if idxNextLt != -1:
						bufi = buf[0:idxNextLt].strip()
						out("bufi: "+bufi)
						if bufi[0:3] == 'KIN':
							out("KIN detected - put these to failover section")
							bufi = str(FAILOVER)
						try:
							foundPageNumber = int(bufi)
							foundPageNumber2 = foundPageNumber
						except:
							out("cannot convert '%s' to integer - give last try..."%bufi)
							try:
								bufi = re.sub("[^0-9]", "", bufi)
								foundPageNumber2 = FAILOVER
								foundPageNumber = int(bufi)
							except: out("IMPOSSIBLE: '%s'"%bufi)
					else:
						out("Spitze Klammer nicht gefunden")
					#
					out("FOUND PAGE NUMBER: Memorize Page-PDF Hit: %i Page: %i (%s %i/%i)..." 
						% (i,foundPageNumber,topic,callerIndex,callerTotal) )
					memorizeUrl(foundPageNumber, foundPageNumber2,
								urlStartIdx, baseUrl + urlStringReplace(urlFragment), callerIndex, topic)
				else:
					out("cannot find end of url")
					continue
			else:
				out("cannot find <a...")
				continue
		else: 
			out("cannot find topic '%s' beyond idx: %i (already found: %i): "%(lookupTopic,idxFoundPrev,i-1))
			break # just to be sure
	#
	out("DONE with topic %s\n"%topic)
#
#
# Ruft myUrl auf, extrahiert das URL-Fragment für den Download, bereinigt es,
# setzt es mit baseUrl zusammen und lädt das PDF herunter. Stellt sicher, dass
# folders existieren und legt darin fileName mit dem heruntergeladenen content an.
def getPdf(c, baseUrl, myUrl, headers, folders, fileName):
	#out("\n\n>>>CALLING '%s' to '%s'""" % (myUrl,fileName))
	out("\n\n>>>CALLING '%s'" % (myUrl))
	r = c.get(myUrl, headers=headers, cookies=c.cookies) 
	out("############################## >>> url: " + r.url + " <<<")
	out("r.status_code: " + str(r.status_code) + " - now finding iframe...")
	#
	ws = r.text
	urlFragment = ""
	lookupTopic = '<iframe id="GS_PagePDF" src="'
	idxFound = ws.find(lookupTopic)
	if idxFound != -1:
		idxFound += len(lookupTopic)
		out("idxFound: "+str(idxFound)+"; len(ws): "+str(len(ws)))
		idxEndUrl = ws.find('"',idxFound)
		if idxEndUrl != -1:
			urlFragment = ws[idxFound : idxEndUrl]
			out("EXTRACTED URL FRAGMENT (" + str(idxFound) + "," + str(idxEndUrl) + "): " + urlFragment)
		else:
			out("cannot find end of url")
	else: 
		out("cannot find iframe")
	#
	myUrl = baseUrl + urlStringReplace(urlFragment) 
	#
	out("\n\n>>>CALLING '%s'" % (myUrl))
	r = c.get(myUrl, headers=headers, cookies=c.cookies) 
	out("############################## >>> url: " + r.url + " <<<")
	out("r.status_code: " + str(r.status_code)) 
	if r.status_code != 200:
		fileName += (".http%i.fail"%r.status_code)
	# 
	out("creating folder(s): " + folders)
	if not os.path.exists(folders): os.makedirs(folders)
	out("opening file: " + fileName)
	fd = open(folders+fileName, 'wb')
	out("writing content of length: %i"%(len(r.content)))
	fd.write(r.content)
	fd.close()
	out("Finished Download - File '%s' created.\n"%(folders+fileName))
#
##########
def loginAndNavigateToToday( username, password, adh, c ):
	out("Login and navigate to main...")
	showAndroidSnack(adh,"Authentifizierung...")
	#
	myUrl = 'http://emedia1.bsb-muenchen.de/han/SZ' # ist noch nicht https und ohne headers
	out("\n\n>>>CALLING: " + myUrl)
	r = c.get(myUrl) # erster Call scheint unnützt, ist aber notwendig
	out("############################## >>> forwarded-url: " + r.url + " <<<")
	out("r.status_code: " + str(r.status_code))
	out("c.cookies:\n" + '\n '.join(map(str,c.cookies)))
	out("r.headers:\n" + '\n '.join('{}: {}'.format(k, v) for k, v in r.headers.items()))
	#
	out("Starte Authentifzierung...")
	# prepare header + data=payload - we work with one combined set for all calls
	headers = {
		'Content-Type': 'application/x-www-form-urlencoded'
		,'Origin': 'https://emedia1.bsb-muenchen.de'
		,'Referer': 'https://emedia1.bsb-muenchen.de/login/bsbLogin.html'
		,'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.88 Safari/537.36'
	}
	payload = {'user': username
		,'plainuser': username
		,'password': password
		,'Service': ''
		,'auth': 'auto'
		,'j_username': 'performIpLogin'
		,'j_password': ''
		,'ipLoginApplication': 'LIBNET'
		,'loginType': ''
			   }
	#
	myUrl = 'https://emedia1.bsb-muenchen.de/hhauth/login' # könnte man sich auch aus der response holen
	out("\n\n>>>CALLING: " + myUrl)
	r = c.post(myUrl, headers=headers, data=payload, cookies=c.cookies, allow_redirects=True)
	myUrl = r.url
	out("############################## >>> forwarded-url: " + r.url + " <<<")
	out("r.status_code: " + str(r.status_code))
	out("c.cookies:\n" + '\n '.join(map(str,c.cookies)))
	out("r.headers:\n" + '\n '.join('{}: {}'.format(k, v) for k, v in r.headers.items()))
	#
	try:
		myUrl = 'https://' + [k.domain for k in c.cookies if k.name=='JSESSIONID'][0] + \
				[k.path   for k in c.cookies if k.name=='JSESSIONID'][0] + '/j_security_check'
	except IndexError as e:
		out("EXC: Session-Cookie not found: "+str(e))
		raise Exception("Auth. failed - check user/pw")
	#
	out("\n\n>>>CALLING: " + myUrl)
	r = c.post(myUrl, headers=headers, data=payload, cookies=c.cookies, allow_redirects=True)
	out("############################## >>> forwarded-url: " + r.url + " <<<")
	out("r.status_code: " + str(r.status_code))
	out("c.cookies:\n" + '\n '.join(map(str,c.cookies)))
	out("r.headers:\n" + '\n '.join('{}: {}'.format(k, v) for k, v in r.headers.items()))
	#
	if( r.status_code == 200 ):
		showAndroidSnack(adh,"Lese Hauptausgabe...")
	#
	out("Suche URL zu den Ganzseiten...")
	# Find within this string the URL:
	# <a target="diz_pdfNavigation" href="https://service3-1szarchiv-1de-10083dcqf023f.emedia1.bsb-muenchen.de/hh03/hh03.ashx?req=nav&uid=libnetbsbmuenchen&ugr=ugroup_abo_libnetretro&bid=navj.SZ.SZ.2019&z=Z44134" onclick="SSP.Navigation.logHeaderLink('Ganzseiten');">Ganzseiten</a>
	ws = r.text
	extractedUrlGanzseiten = ""
	idxGanzseiten = ws.find('" onclick="SSP.Navigation.logHeaderLink(\'Ganzseiten\');"')
	if idxGanzseiten != -1:
		out("idxGanzseiten: "+str(idxGanzseiten)+"; len(ws): "+str(len(ws)))
		idxARef = ws.rfind('<a target="diz_pdfNavigation" href="',idxGanzseiten-500,len(ws)-idxGanzseiten)
		if idxARef != -1:
			extractedUrlGanzseiten = ws[idxARef+36 : idxGanzseiten]
			out("EXTRACTED URL (" + str(idxARef) + "): " + extractedUrlGanzseiten)
		else:
			out("cannot find <a... - quitting...")
			quit()
	else:
		out("cannot find Ganzseiten - quitting...")
		quit()
	#
	myUrl = extractedUrlGanzseiten
	out("\n\n>>>CALLING Ganzseiten: " + myUrl)
	r = c.get(myUrl, headers=headers, cookies=c.cookies)
	out("############################## >>> forwarded-url: " + r.url + " <<<")
	out("r.status_code: " + str(r.status_code))
	out("c.cookies:\n" + '\n '.join(map(str,c.cookies)))
	out("r.headers:\n" + '\n '.join('{}: {}'.format(k, v) for k, v in r.headers.items()))
	#
	baseUrl = myUrl[ : myUrl.find('/hh03/')+6]
	#
	r.encoding = "UTF-8"
	ws = r.text.replace("&amp;","&")
	out("r.text: \n" + ws)
	#
	# by *default* we are on SZ Hauptausgabe
	#
	# <div style="color:#C35100; border-bottom:1px solid Gainsboro; margin-bottom:7px; padding-bottom:5px; ">
	# <a href="hh03.ashx?req=nav&bid=navj.SZ.SZ.2020...&uid=libnetbsbmuenchen&usi=10042&ugr=ugroup%5Fabo%5Flibnetretro&z=Z38846">SZ-Hauptausgabe</a></div>
	#
	#
	# subnavi zum magazin könnte mit diesem Link zu funktionieren:
	#
	# <div style="border-bottom:1px solid Gainsboro; margin-bottom:7px; padding-bottom:5px; ">
	# <a title="11.05.1990 - heute" href="hh03.ashx?req=nav&bid=navj.SZ.MAG.2020...&uid=libnetbsbmuenchen&usi=10042&ugr=ugroup%5Fabo%5Flibnetretro&z=Z91361">SZ Magazin</a></div>
	#
	out("Suche URL des letzten Erscheinungstags...")
	# parsing for today or most recent within
	# <a title="Letzter Erscheinungstag: 27.12.2019" href="hh03.ashx?req=nav&amp;bid=navd.SZ.SZ.20191227.DEU....&amp;uid=libnetbsbmuenchen&amp;usi=10017&amp;ugr=ugroup%5Fabo%5Flibnetretro&amp;z=Z23777">Â heute</a></div>
	extractedUrlRelative = ""
	idxHeute = ws.find('Letzter Erscheinungstag')
	if idxHeute != -1:
		idxARef = ws.find('href="',idxHeute)
		if( idxARef != -1 ):
			idxEndUrl = ws.find('"',idxARef+7)
			if idxEndUrl != -1:
				out("idxHeute: %i, idxARef: %i, idxEndUrl: %i"%(idxHeute,idxARef,idxEndUrl))
				extractedUrlRelative = ws[idxHeute+43 : idxEndUrl]
			else:
				out("cannot find terminator")
		else:
			out("cannot find href")
	else:
		out("cannot find Letzter Erscheinungstag")
	#
	# letzter (auf der Seite) ET1A *mit* a-tag-child. Vorsicht gibt auch ET1a und child; Außerdem ist noch Space dazwischen
	# Logik: Alle Spaces wegschmeißen und dann auf '<tdclass="ET1A"><ahref="' von hinten losgehen
	# <td class="ET1A">									<a href="hh03.ashx?req=nav&amp;bid=navd.SZ.SZ.20200102.....&amp;uid=libnetbsbmuenchen&amp;usi=10017&amp;ugr=ugroup%5Fabo%5Flibnetretro&amp;z=Z78060">2</a></td>
	if extractedUrlRelative == "":
		out("letzter Versuch")
		ws = r.text.replace(' ','').replace('\t','').replace('\n','').replace('\r','')
		suchTok = '<tdclass="ET1A"><ahref="'
		idxHeute = ws.rfind(suchTok)
		if idxHeute != -1:
			idxARef = 0
			if( idxARef != -1 ):
				idxEndUrl = ws.find('"',idxHeute+len(suchTok))
				if idxEndUrl != -1:
					out("Gefunden - extrahiere URL; idxHeute: %i, idxARef: %i, idxEndUrl: %i"%(idxHeute,idxARef,idxEndUrl))
					extractedUrlRelative = ws[idxHeute+len(suchTok) : idxEndUrl]
				else:
					out("cannot find terminator")
			else:
				out("cannot find href")
		else:
			out("cannot find '%s'"%suchTok)
	#
	if extractedUrlRelative == "":
		out("quitting...")
		quit()
	#
	myUrl = baseUrl + extractedUrlRelative
	out( "replacing strings" )
	myUrl = urlStringReplace(myUrl)
	out("\n\n>>>CALLING (Subnavi nach HEUTE): " + myUrl)
	r = c.get(myUrl, headers=headers, data=payload, cookies=c.cookies, allow_redirects=True)
	myUrl = r.url
	out("############################## >>> forwarded-url: " + r.url + " <<<")
	out("r.status_code: " + str(r.status_code))
	out("c.cookies:\n" + '\n '.join(map(str,c.cookies)))
	out("r.headers:\n" + '\n '.join('{}: {}'.format(k, v) for k, v in r.headers.items()))
	out("r.request.headers:\n" + '\n '.join('{}: {}'.format(k, v) for k, v in r.request.headers.items()))
	#
	out("returning baseUrl: '%s'"%(baseUrl))
	return(r,baseUrl,headers,payload)
#
# WebSite ws should contain a string like this:
# <a title="11.05.1990 - heute" href="hh03.ashx?req=nav&bid=navd.SZ.MAG.20200430.....&uid=libnetbsbmuenchen&usi=10006&ugr=ugroup%5Fabo%5Flibnetretro&z=Z52508" style="color:black; ">SZ Magazin</a></div>
# This function matches i.e. "SZ Magazin" and returns the urlFragment within href attribute
def getSubNaviUrl(ws,sArea):
	rcUrlFragment = ""
	lookupATxt = "%s</a>"%sArea
	lookupARef = 'href="'
	lookupTerm = '"'
	idxFound = ws.find(lookupATxt)
	if idxFound != -1:
		out("a-text found at idx: "+str(idxFound))
		idxARef = ws.rfind(lookupARef,idxFound-500,idxFound-1)
		if idxARef != -1:
			out("a-href found at idx: "+str(idxARef))
			idxTerm = ws.find(lookupTerm,idxARef+10)
			if idxTerm != -1:
				out("terminator found at idx: "+str(idxTerm))
				rcUrlFragment = urlStringReplace(ws[idxARef+len(lookupARef) : idxTerm])
	out("returning subnavi-url: '%s'; ATxt: '%s'; len ws: %i"%(rcUrlFragment,lookupATxt,len(ws)))
	return( rcUrlFragment )
#
##########
# This executor is operated differently depending on the platform it is running on
# If on android: adh is an object of kotlin class AsyncDownloadHandler. It shows progress advance.
# If not on android: for adh pass in an arbitrary string (object is ignored)
##########
def executeScript( adh, strContext ):
	out("Starte Ausführung (%s) des Scripts..."%strContext)
	username,password,myPages,myTopics,downloadFolder = load_config()
	check_config(username,password,myPages,myTopics,downloadFolder) # will raise on error
	#
	dictDownloads.clear()
	#
	out("Starte Session...")
	# the session will gather the cookies and hold them for preceeding calls
	with requests.Session() as c: # "connection"
		# apply WORKAROUND "android qpython cipher", see above
		c.mount('https://', MyAdapter())
		#
		r,baseUrl,headers,payload = loginAndNavigateToToday(username,password,adh,c)
		#
		r.encoding = "UTF-8"
		ws = r.text.replace("&amp;","&")
		out("r.text: \n" + ws)
		#
		# We reached today (or most recent).
		# By default we are on SZ Hauptausgabe - sub-navi is available:
		#
		# <a title="29.01.1998 - heute" href="hh03.ashx?req=nav&bid=navd.SZ.EXT.20200430.....&uid=libnetbsbmuenchen&usi=10006&ugr=ugroup%5Fabo%5Flibnetretro&z=Z55742" style="color:black; ">SZ Extra</a></div>
		# <div style="border-bottom:1px solid Gainsboro; margin-bottom:7px; padding-bottom:5px; ">
		# <a title="11.05.1990 - heute" href="hh03.ashx?req=nav&bid=navd.SZ.MAG.20200430.....&uid=libnetbsbmuenchen&usi=10006&ugr=ugroup%5Fabo%5Flibnetretro&z=Z52508" style="color:black; ">SZ Magazin</a></div>
		# <div style="border-bottom:1px solid Gainsboro; margin-bottom:7px; padding-bottom:5px; ">
		# <a title="04.01.1999 - heute" href="hh03.ashx?req=nav&bid=navd.SZ.REG.20200430.....&uid=libnetbsbmuenchen&usi=10006&ugr=ugroup%5Fabo%5Flibnetretro&z=Z96586" style="color:black; ">SZ-Landkreise</a></div></td>
		#
		# todo: Regel "überhaupt angefordert" hinzufügen
		# myUrl = getSubNaviUrl(ws, "Magazin")
		# if len(myUrl) > 0:
		# 	myUrl = baseUrl + myUrl
		# 	out("\n\n>>>CALLING (Subnavi zum Magazin): " + myUrl)
		# 	r = c.get(myUrl, headers=headers, data=payload, cookies=c.cookies, allow_redirects=True)
		# 	myUrl = r.url
		# 	out("############################## >>> forwarded-url: " + r.url + " <<<")
		# 	out("r.status_code: " + str(r.status_code))
		# 	#
		# 	r.encoding = "UTF-8"
		# 	ws = r.text.replace("&amp;","&")
		# 	out("r.text: \n" + ws)
			#
		# Starting downloads, first by page, then by topic
		out("\n-----------------------------\n--------- GET PAGES ---------\n-----------------------------")
		i = 0 # each collection
		ao = 0 # allover
		#
		#myPages = ['97','1','2','23','63','64','16']
		for m in myPages:
			i += 1
			ao += 1
			out("Processing PDFs by page %i/%i ..." % (i,len(myPages)) )
			urlStartIdx, myUrl = getUrlByPageNumber(ws, m, baseUrl)
			if urlStartIdx == -1:
				out("PDFs by page %i/%i was not found - skipping" % (i,len(myPages)) )
			else:
				out("Memorize Page-PDF (%i/%i)..." % (i,len(myPages)) )
				memorizeUrl(m, m, urlStartIdx, myUrl, i)
		out("done with pages, now %i:\n %s"%(len(dictDownloads),'\n '.join('{}: {}'.format("%02i"%(k), "[%i] %s"%(v['pos'],v['url'][96:999])) for k, v in dictDownloads.items())))
		#
		out("\n------------------------------\n--------- GET TOPICS ---------\n------------------------------")
		i = 0
		#myTopics = ['Politik','GibtsNet','Feuilleton','Themen des Tages','Die Seite Drei']
		for m in myTopics:
			i += 1
			out("Processing PDFs by topic %i/%i (%s)..." % (i,len(myTopics),m) )
			getUrlByTopic(ws, m, baseUrl, i, len(myTopics))
		out("done with topics, now %i:\n %s"%(len(dictDownloads),'\n '.join('{}: {}'.format("%02i"%(k), "[%i] %s"%(v['pos'],v['url'][96:999])) for k, v in dictDownloads.items())))
		#
		out("\n------------------------------\n--------- DOWNLOAD ---------\n------------------------------")
		showAndroidSnack(adh,"%i PDF holen..."%len(dictDownloads))
		i = 0
		for k in dictDownloads:
			i += 1
			myUrl = dictDownloads[k]['url']
			out( "Downloading PDFs SZ-Page: %i Progress: %i/%i ...\nURL:%s"%(k,i,len(dictDownloads),myUrl) )
			getPdf( c, baseUrl, myUrl, headers, downloadFolder, getFileNameForPdf(k,i) )
			if(i%10==0):
				showAndroidSnack(adh,"Hole %i/%i PDF..."%(i,len(dictDownloads)))
		#
		out( "done with downloading %i PDFs"%len(dictDownloads) )
		showAndroidSnack(adh,"Fertig. %i PDF geholt."%len(dictDownloads))
		#
	out("done with executeScript.")
	out("Beende Ausführung (%s) des Scripts"%strContext)
	#
if(is_android()):
	out("MARK THIS: no automatic execution on loading script when invoked on android")
else:
	out("calling executeScript")
	executeScript("adh","we are not on android, script is executed on invoking script")
out("done with script.")
#
