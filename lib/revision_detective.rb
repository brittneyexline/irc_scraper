require 'detective.rb'
require 'mediawiki_api.rb'

class RevisionDetective < Detective
  def table_name
    'revision'
  end

  #return a proc that defines the columns used by this detective
  #if using this as an example, you probably should copy the first two columns (the id and foreign key)
  def columns
    Proc.new do
      <<-SQL
      id integer primary key autoincrement,
      revision_id integer,                              --foreign key to reference the original revision
      is_minor integer,
      timestamp timestamp,
      user string,
      comment string,
      size integer,
      rev_content string,
      num_links_added integer,
      --tags 
      FOREIGN KEY(revision_id) REFERENCES irc_wikimedia_org_en_wikipedia(id)   --TODO this table name probably shouldn't be hard coded
      --FOREIGN KEY(user) REFERENCES irc_wikimedia_org_en_wikipedia(user) --TODO
SQL
    end
  end

  #info is a list: 
  # 0: sample_id (string), 
  # 1: article_name (string), 
  # 2: desc (string), 
  # 3: rev_id (string),
  # 4: old_id (string)
  # 5: user (string), 
  # 6: byte_diff (int), 
  # 7: timestamp (Time object), 
  # 8: description (string)
  def investigate info
    
    #http://en.wikipedia.org/w/api.php?action=query&prop=revisions&revid=info[3]&rvprop=timestamp|user|comment|size&rvlimit&vdiffto=prev
    
    revinfo = find_revision_info(info)
    
    db_write!(
      ['revision_id', 'timestamp', 'user', 'comment', 'size', 'rev_content', 'is_minor', 'num_links_added'],
      [info[0]] + revinfo
    )
  end
  
  def find_revision_info info
    
    xml = get_xml({:format => :xml, :action => :query, :prop => :revisions, :revids => info[3], :rvprop => 'timestamp|user|comment|size|flags', :rvdiffto => :prev})
    res = parse_xml(xml)

    timestamp = Time.parse(res.first['pages'].first['page'].first['revisions'].first['rev'].first['timestamp'])

    user = res.first['pages'].first['page'].first['revisions'].first['rev'].first['user']

    comment = res.first['pages'].first['page'].first['revisions'].first['rev'].first['comment']

    size = res.first['pages'].first['page'].first['revisions'].first['rev'].first['size']

    rev_content = res.first['pages'].first['page'].first['revisions'].first['rev'].first['diff']

    flag = res.first['pages'].first['page'].first['revisions'].first['rev'].first['minor']

     if (flag=="")
       is_minor = 1
     else
       is_minor = 0
     end

    #http://en.wikipedia.org/w/api.php?action=query&prop=extlinks&revids=800129
    xml2= get_xml({:format => :xml, :action => :query, :prop => :extlinks, :revids => info[3]})
    res2 = parse_xml(xml2)
    links_new = res2.first['pages'].first['page'].first['extlinks']
    if(links_new != nil)
	    links_new = links_new.first['el']
    else
	    links_new = []
    end
    #convert the array of hashes into just an array of links
    links_new.collect! do |link|
      link['content']
    end

    xml2= get_xml({:format => :xml, :action => :query, :prop => :extlinks, :revids => info[4]})
    res2 = parse_xml(xml2)
    links_old = res2.first['pages'].first['page'].first['extlinks']

    if(links_old != nil)
	    links_old = links_old.first['el']
    else
	    links_old = []
    end
    links_old.collect! do |link|
      link['content']
    end
    
    linkdiff = links_new - links_old #this doesn't work, links_new a hash or array?
    #links_new.first['content']

    #linkdiff = []
    #links_new.each do |link|
    #  unless links_old.include?(link)
    #    linkdiff << link
    #  end
    #end
    
    [timestamp.to_i, user.to_s, comment.to_s, size.to_i, rev_content.to_s, is_minor.to_i, linkdiff.length.to_i]
    
  end
  
end