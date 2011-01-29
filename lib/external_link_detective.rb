require 'detective.rb'
require 'mediawiki_api.rb'
require 'uri'
require 'cgi'
require 'net/http'
require 'alexa_config.rb'
require 'alexa_urlinfo.rb'
require 'base64'
require 'bundler/setup'
require 'nokogiri'

class ExternalLinkDetective < Detective
  def self.table_name
    'link'
  end

  #return a proc that defines the columns used by this detective
  #if using this as an example, you probably should copy the first two columns (the id and foreign key)
  def self.columns
    Proc.new do
      <<-SQL
      id integer primary key autoincrement,
      revision_id integer,
      link text,
      headers text,
      source text,
<<<<<<< HEAD
<<<<<<< HEAD
      screenshot text,
      phishing text,
      malware text,
      FOREIGN KEY(revision_id) REFERENCES irc_wikimedia_org_en_wikipedia(id)   --TODO this table name probably shouldnt be hard coded
=======
      site_description string,
=======
      description text,
      created DATE DEFAULT (datetime('now','localtime')),
>>>>>>> 65e3811614296ced54ba752031d78e95d92ad49e

      --stuff from alexa--
      title string,
      site_description string,
      online_since timestamp,
      speed_medianloadtime decimal, 
      speed_percentile decimal,
      adult_content integer,
      language_locale string,
      language_encoding string,
      links_in_count integer,
      keywords string,
      num_related_links integer,
      related_links string,

      rank integer,
      rank_delta decimal,
      reach_rank integer, 
      reach_rank_delta decimal,
      reach_permill decimal, 
      reach_permill_delta decimal,
      views_permill decimal,
      views_permill_delta decimal,
      views_rank integer,
      views_rank_delta decimal,
      views_peruser decimal, 
      views_peruser_delta decimal,

      rank_by_city string,
      rank_by_country string,

<<<<<<< HEAD
      FOREIGN KEY(revision_id) REFERENCES irc_wikimedia_org_en_wikipedia(id)
>>>>>>> f0daf8990dcba7028ed1e4f3fd2fd1dec2e78a83
=======
      --stuff from google malware--
      screenshot text,
      phishing text,
      malware text,
      --FOREIGN KEY(revision_id) REFERENCES irc_wikimedia_org_en_wikipedia(revision_id)   --TODO this table name probably shouldnt be hard coded

>>>>>>> 65e3811614296ced54ba752031d78e95d92ad49e
SQL
    end
  end

  #info is a list: 
  # 0: article_name (string), 
  # 1: desc (string), 
  # 2: rev_id (string),
  # 3: old_id (string)
  # 4: user (string), 
  # 5: byte_diff (int), 
  # 6: description (string)
  # 7: diff_unescaped_xml (string)
  # 8: attributes from call: user, timestamp, revid, size, title, from, to, parentid, anon, ns, space, pageid
  # 9: tags (Array)
  # 10: array of array of links found in [url, desc] format, description may be nil if it was not a wikilink
  def investigate info
    linkarray = info.last
    
<<<<<<< HEAD
    linkarray.each do |linkentry|
      rownum = db_write!(
<<<<<<< HEAD
        ['revision_id', 'link', 'source', 'screenshot', 'phishing', 'malware'],
	      [info[0], linkentry["link"], linkentry["source"], linkentry['screenshot'], linkentry['phishing'], linkentry['malware']]
	    )
=======
        ['revision_id', 'link', 'source', 'site_description', 'title', 'description', 'online_since', 'speed_medianloadtime', 'speed_percentile', 'adult_content', 'language_locale',
=======
    results = []
    linkarray.each do |arr|
      #puts arr.first
      source_content_error, headers = find_source(arr.first)
      headers_str = Marshal.dump(headers)
      #ignore binary stuff for now
      linkinfo = find_alexa_info(arr.first)
      link = arr.first
      imname = link.delete "."
      imname = imname.delete "/"
      imnaame = imname + '.jpg'
      request = "http://api1.thumbalizr.com/?url=" + link + "&width=300"
      resp2 = Net::HTTP.get_response(URI.parse(request))
      file = File.open( imname , 'wb' )
      file.write(resp2.body)
      #all of andrews malware code needs to be in the same directory as this file,
      #it doesn't run the command properly when you try to call the file with the pathname
      malware_info = find_malware_info(link)
      
      results << { 
        :link => arr.first, 
        :source => ['gzip', 'deflate', 'compress'].include?(headers['Content-encoding']) ? 'encoded' : source_content_error, 
        :description => arr.last, 
        :headers => headers_str,
        :linkinfo => linkinfo,
        :screenshot => imname,
        :phishing => malware_info[0],
        :malware => malware_info[1]
      }
    end

    results.each do |linkentry|
      db_queue(
        ['revision_id', 'link', 'source', 'description', 'headers' 'title', 'site_description', 'online_since', 'speed_medianloadtime', 'speed_percentile', 'adult_content', 'language_locale',
>>>>>>> 65e3811614296ced54ba752031d78e95d92ad49e
      'language_encoding',
      'links_in_count',
      'keywords',
      'num_related_links',
      'related_links',
      'rank',
      'rank_delta',
      'reach_rank',
      'reach_rank_delta',
      'reach_permill',
      'reach_permill_delta',
      'views_permill',
      'views_permill_delta',
      'views_rank',
      'views_rank_delta','views_peruser','views_peruser_delta',
      'rank_by_city',
<<<<<<< HEAD
      'rank_by_country'],
	      [info[0], linkentry["link"], linkentry["source"], linkentry["description"]] + linkentry["linkinfo"]    )
>>>>>>> f0daf8990dcba7028ed1e4f3fd2fd1dec2e78a83
=======
      'rank_by_country' 'screenshot', 'phishing', 'malware'],
      [info[2], linkentry[:link], linkentry[:source], linkentry[:description], linkentry[:headers]] +
        linkentry[:linkinfo] +
        [linkentry[:screenshot], linkentry[:phishing], linkentry[:malware]] )
>>>>>>> 65e3811614296ced54ba752031d78e95d92ad49e
    end	
    true # :)
  end	
  
  #return either the source, a non text/html contenttype or the httperror class, all as strings
  def find_source(url)
    #TODO do a check for the size and type-content of it before we pull it
    #binary files we probably don't need to grab and things larger than a certain size we don't want to grab
    uri = URI.parse(url)
    
    http = Net::HTTP.new(uri.host)
    resp = nil
    begin
      path = uri.path.to_s.empty? ? '/' : "#{uri.path}?#{uri.query}"
      resp = http.request_get(path, 'User-Agent' => 'WikipediaAntiSpamBot/0.1 (+hincapie.cis.upenn.edu)')
    rescue SocketError => e
      resp = e
    end
    
<<<<<<< HEAD
<<<<<<< HEAD
    linkarray = []
    linkdiff.each do |link|
       response = Net::HTTP.get_response URI.parse(link)
       if (response = Net::HTTPRedirection)
         link = response['location']
         response = Net::HTTP.get_response URI.parse(link)
       end
       if(response.content_type != "text/html")
         source = ""
       else
         source = Net::HTTP.get URI.parse(link)
       end
       #TODO do a check for the size and type-content of it before we pull it
       #binary files we probably don't need to grab and things larger than a certain size we don't want to grab
       if(source.length > 100000)
         source = source[0,100000]
       end
       #not sure exactly where to store the images, probably should have their own directory
       imname = link.delete "."
       imname = imname.delete "/"
       imnaame = imname + '.jpg'
       request = "http://api1.thumbalizr.com/?url=" + link + "&width=300"
       resp2 = Net::HTTP.get_response(URI.parse(request))
       file = File.open( imname , 'wb' )
       file.write(resp2.body)
       #all of andrews malware code needst to be in the same directory as this file, 
      #it doesn't run the command properly when you try to call the file with the pathname
       sbinfo = `java test #{link}`
       sbinfo = sbinfo.chomp
       sbarr = sbinfo.partition(",")
       phishing = sbarr[0]
       malware = sbarr[2]
       linkarray << {"link" => link, "source" => source, "image" => imname, "phishing" => isphishing, "malware" => ismalware}
=======
    ret = []
    if(resp.is_a? Net::HTTPOK or resp.is_a? Net::HTTPFound)
      if resp.content_type == 'text/html'
        #puts resp.body.length
        ret << resp.body[0..10**5] #truncate at 100K characters; not a good way to deal with size, should check the headers only
      else
        ret << resp.content_type
      end
      ret << true
    else
      ret << resp.class.to_s
      ret << false
>>>>>>> f0daf8990dcba7028ed1e4f3fd2fd1dec2e78a83
=======
    ret = []
    if(resp.is_a? Net::HTTPOK or resp.is_a? Net::HTTPFound)
      #truncate at 100K characters; not a good way to deal with size, should check the headers only
      #else set the body to the content type
      if resp.content_type == 'text/html'
        ret << resp.body[0..10**5]
      else
        ret << resp.content_type
      end
    else #TODO follow redirects!
      #if it's a bad http response set the body equal to that response
      ret << resp.class.to_s
>>>>>>> 65e3811614296ced54ba752031d78e95d92ad49e
    end
    ret << resp.to_hash #the headers
    ret
  end
  
def find_alexa_info(link)
      Alexa.config do |c|
          c.access_key_id = ALEXA_KEY_ID
	  c.secret_access_key = ALEXA_SECRET_KEY
      end
      linkinfo = Alexa::UrlInfo.new(:host => link)
      xml = linkinfo.connect
      linkinfo.parse_xml(xml)
       rank, rank_delta, reach_rank, reach_rank_delta, reach_permill, 
         reach_permill_delta, views_permill, views_permill_delta, views_rank,
         views_rank_delta, views_peruser, views_peruser_delt = nil
      stats = linkinfo::usage_statistics
      if stats
      	 if !stats.empty?
            if !stats.first['Rank'].empty?
                rank = stats.first['Rank']['Value']
		rank_delta = stats.first['Rank']['Delta']
            end
            if !stats.first['Reach'].empty?
	       if !stats.first['Reach']['Rank'].empty?
                reach_rank = stats.first['Reach']['Rank']['Value']
		reach_rank_delta = stats.first['Reach']['Rank']['Delta']
               end
	       if !stats.first['Reach']['PerMillion'].empty?
                reach_permill = stats.first['Reach']['PerMillion']['Value']
		rank_permill_delta = stats.first['Reach']['PerMillion']['Delta']
               end	       
	    end       
            if !stats.first['PageViews'].empty?
	       if !stats.first['PageViews']['Rank'].empty?
                views_rank = stats.first['PageViews']['Rank']['Value']
		views_rank_delta = stats.first['PageViews']['Rank']['Delta']
               end
	       if !stats.first['PageViews']['PerMillion'].empty?
                views_permill = stats.first['PageViews']['PerMillion']['Value']
		views_permill_delta = stats.first['PageViews']['PerMillion']['Delta']
	       end
	       if !stats.first['PageViews']['PerUser'].empty?
                views_peruser = stats.first['PageViews']['PerUser']['Value']
		views_peruser_delta = stats.first['PageViews']['PerUser']['Delta']
               end	       
	    end      
         end
      end
      
      if linkinfo::adult_content == "no"
         ac = 0
      else
         ac = 1
      end

      [linkinfo::site_title.to_s, linkinfo::site_description.to_s, Time.parse(linkinfo::online_since).to_i, linkinfo::speed_median_load_time.to_f, linkinfo::speed_percentile.to_f, ac, linkinfo::language_locale.to_s, linkinfo::language_encoding.to_s, linkinfo::links_in_count.to_i, Base64.encode64(Marshal.dump(linkinfo::keywords)), linkinfo::related_links.size.to_i, Base64.encode64(Marshal.dump(linkinfo::related_links)), rank.to_i, rank_delta.to_f, reach_rank.to_i, reach_rank_delta.to_f, reach_permill.to_f, reach_permill_delta.to_f, views_permill.to_f, views_permill_delta.to_f, views_rank.to_i, views_rank_delta.to_f, views_peruser.to_f, views_peruser_delta.to_f, Base64.encode64(Marshal.dump(linkinfo::rank_by_city)), Base64.encode64(Marshal.dump(linkinfo::rank_by_country))]
  end

  def find_malware_info(link)
       sbinfo = `java test #{link}`
       sbinfo = sbinfo.chomp
       sbarr = sbinfo.partition(",")
       [bool_to_int(sbarr[0]),bool_to_int(sbarr[2])]
  end

  def bool_to_int(val)
       if (val == "true")
         i = 1
       end
       if (val == "false")
         i = 0
       end
       i
  end

end
