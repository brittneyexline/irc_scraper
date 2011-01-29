require 'detective.rb'
require 'mediawiki_api.rb'

class RevisionDetective < Detective
  def self.table_name
    'revision'
  end

  #return a proc that defines the columns used by this detective
  #if using this as an example, you probably should copy the first two columns (the id and foreign key)
  def self.columns
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
      -- num_links_added integer, --already found from the diff file
      namespace string,
      -- TODO only_link boolean,
      created DATE DEFAULT (datetime('now','localtime')),
      --tags 
      FOREIGN KEY(revision_id) REFERENCES irc_wikimedia_org_en_wikipedia(revision_id)   --TODO this table name probably shouldn't be hard coded
      --FOREIGN KEY(user) REFERENCES irc_wikimedia_org_en_wikipedia(user) --TODO
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
    
    #http://en.wikipedia.org/w/api.php?action=query&prop=revisions&revid=info[3]&rvprop=timestamp|user|comment|size&rvlimit&vdiffto=prev
    
    revinfo = find_revision_info(info)
    if (revinfo.size!=0)
    db_write!(
      ['revision_id', 'timestamp', 'user', 'comment', 'size', 'rev_content', 'is_minor', 'namespace'],
      [info[0]] + revinfo
    )
    end
  end
  
  def find_revision_info info
    if (info[8])
       timestamp = Time.parse(info[8]["timestamp"], "%Y-%m-%dT%H:%M:%SZ")
       user = info[8]["user"]
       comment = info[8]["comment"]
       size = info[8]["size"]
       is_minor = info[8]["minor"]
       parent_id = info[8]["parentid"]
       namespace = info[8]["ns"]
    end
    rev_content = info[7]
    tags = info[9]

#    xml = get_xml({:format => :xml, :action => :query, :prop => :revisions, :revids => info[2], :rvprop => 'ids|tags|flagged|timestamp|user|comment|size|flags', :rvdiffto => :prev})
#    res = parse_xml(xml)
#
#    if(res.first['badrevids'] == nil)
#       rxml = res.first['pages'].first['page'].first['revisions'].first['rev'].first
#       timestamp = find_timestamp(rxml)
#       user = find_user(rxml)
#       comment = find_comment(rxml)
#       size = find_size(rxml)
#       rev_content = find_content(rxml)
#       flag = find_flag(rxml)
#       namespace = find_namespace(res)
#
#       if (flag=="")
#       	  is_minor = 1
#       else
#          is_minor = 0
#       end
#
#       http://en.wikipedia.org/w/api.php?action=query&prop=extlinks&revids=800129
#       xml2= get_xml({:format => :xml, :action => :query, :prop => :extlinks, :revids => info[3]})
#       res2 = parse_xml(xml2)
#       links_new = res2.first['pages'].first['page'].first['extlinks']
#       if(links_new != nil)
#          links_new = links_new.first['el']
#       else
#          links_new = []
#       end
#       #convert the array of hashes into just an array of links
#       links_new.collect! do |link|
#          link['content']
#       end
#
#       xml2 = get_xml({:format => :xml, :action => :query, :prop => :extlinks, :revids => info[4]})
#       res2 = parse_xml(xml2)
#       links_old = []
#       if(res2.first['badrevids'] == nil)
#         links_old = res2.first['pages'].first['page'].first['extlinks']
#         if(links_old != nil)
#           links_old = links_old.first['el']
#         else
#           links_old = []
#         end
#       end
#
#       links_old.collect! do |link|
#         link['content']
#       end
#
#       linkdiff = links_new - links_old

      [timestamp.to_i, user.to_s, comment.to_s, size.to_i, rev_content.to_s, is_minor, namespace, tags, parent_id.to_i]
#    else
#      []
#    end
  end
  
  #rxml = ruby-ified xml
  def find_timestamp rxml
    Time.parse(rxml['timestamp'])
  end
  
  def find_user rxml
    rxml['user']
  end
  
  def find_comment rxml
    rxml['comment']
  end
  
  def find_size rxml
    rxml['size']
  end
  
  def find_content rxml
    rxml['diff']
  end
  
  def find_flag rxml
    rxml['minor']
  end
  
  def find_namespace rxml
    rxml.first['pages'].first['page'].first['ns']
  end
  
end